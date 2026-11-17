package httpapi_test

import (
	"bytes"
	"context"
	"crypto/rand"
	"crypto/rsa"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"math/big"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/audit"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/cache"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/config"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/db"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/httpapi"
)

const (
	frontend     = "ROLE_AAP__FRONTEND_RW"
	coordinator  = "ROLE_AAP__COORDINATOR_RW"
	viewer       = "ROLE_AAP__VIEW_R"
	dataDeletion = "ROLE_AAP_DATA_DELETION"
	sar          = "ROLE_SAR_DATA_ACCESS"
)

// testDatabaseURL is a server the tests can create databases on.
func testDatabaseURL() string {
	if url := os.Getenv("TEST_DATABASE_URL"); url != "" {
		return url
	}
	return "postgres://root:dev@localhost:55433/postgres?sslmode=disable"
}

var signingKey = sync.OnceValue(func() *rsa.PrivateKey {
	key, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		panic(err)
	}
	return key
})

type testApp struct {
	t      *testing.T
	server *httptest.Server
	pool   *pgxpool.Pool
}

// newTestApp starts the API against a fresh database, with a stand-in for HMPPS Auth that
// serves the public key test tokens are signed with.
func newTestApp(t *testing.T) *testApp {
	t.Helper()
	ctx := context.Background()

	admin, err := pgx.Connect(ctx, testDatabaseURL())
	if err != nil {
		t.Skipf("no test database available at %s: %v", testDatabaseURL(), err)
	}
	name := fmt.Sprintf("test_%d", time.Now().UnixNano())
	if _, err := admin.Exec(ctx, "CREATE DATABASE "+name); err != nil {
		t.Fatal(err)
	}

	url := strings.Replace(testDatabaseURL(), "/postgres?", "/"+name+"?", 1)
	pool, err := db.Connect(ctx, config.Database{URL: url, Schema: "public", PoolSize: 5}, "Europe/London")
	if err != nil {
		t.Fatal(err)
	}
	if err := db.Migrate(ctx, pool); err != nil {
		t.Fatal(err)
	}

	auth := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/auth/.well-known/jwks.json" {
			http.NotFound(w, r)
			return
		}
		public := signingKey().PublicKey
		_ = json.NewEncoder(w).Encode(map[string]any{"keys": []map[string]string{{
			"kty": "RSA", "kid": "test-key", "use": "sig", "alg": "RS256",
			"n": base64.RawURLEncoding.EncodeToString(public.N.Bytes()),
			"e": base64.RawURLEncoding.EncodeToString(big.NewInt(int64(public.E)).Bytes()),
		}}})
	}))

	appCtx, cancel := context.WithCancel(ctx)
	cfg := config.Config{ServiceName: "test", HmppsAuthURL: auth.URL + "/auth", MaxLookbackDays: 1}
	app, err := httpapi.New(appCtx, cfg, pool, audit.Disabled("test"), cache.Disabled())
	if err != nil {
		t.Fatal(err)
	}
	server := httptest.NewServer(app.Handler())

	t.Cleanup(func() {
		server.Close()
		cancel()
		auth.Close()
		pool.Close()
		_, _ = admin.Exec(ctx, "DROP DATABASE "+name+" WITH (FORCE)")
		admin.Close(ctx)
	})
	return &testApp{t: t, server: server, pool: pool}
}

func token(roles ...string) string {
	now := time.Now()
	claims := jwt.MapClaims{
		"sub": "test-client", "client_id": "test-client", "authorities": roles,
		"iat": now.Unix(), "exp": now.Add(5 * time.Minute).Unix(), "aud": "oauth2-resource",
	}
	signer := jwt.NewWithClaims(jwt.SigningMethodRS256, claims)
	signer.Header["kid"] = "test-key"
	signed, err := signer.SignedString(signingKey())
	if err != nil {
		panic(err)
	}
	return signed
}

// request sends a request, returning the status and the decoded JSON body (or the raw text).
func (a *testApp) request(method, uri string, roles []string, body any) (int, any) {
	a.t.Helper()
	var reader io.Reader
	if body != nil {
		encoded, _ := json.Marshal(body)
		reader = bytes.NewReader(encoded)
	}
	request, _ := http.NewRequest(method, a.server.URL+uri, reader)
	if len(roles) > 0 {
		request.Header.Set("Authorization", "Bearer "+token(roles...))
	}
	request.Header.Set("Content-Type", "application/json")
	response, err := http.DefaultClient.Do(request)
	if err != nil {
		a.t.Fatal(err)
	}
	defer response.Body.Close()
	raw, _ := io.ReadAll(response.Body)
	var decoded any
	if json.Unmarshal(raw, &decoded) != nil {
		decoded = string(raw)
	}
	return response.StatusCode, decoded
}

func (a *testApp) commandsAt(uri string, commands ...any) any {
	a.t.Helper()
	status, body := a.request(http.MethodPost, uri, []string{frontend}, map[string]any{"commands": commands})
	if status != http.StatusOK {
		a.t.Fatalf("commands failed with %d: %v", status, body)
	}
	return body
}

func (a *testApp) commands(commands ...any) any { return a.commandsAt("/command", commands...) }

func (a *testApp) query(q map[string]any) any {
	a.t.Helper()
	status, body := a.request(http.MethodPost, "/query", []string{viewer}, map[string]any{"queries": []any{q}})
	if status != http.StatusOK {
		a.t.Fatalf("query failed with %d: %v", status, body)
	}
	return at(body, "queries", 0, "result")
}

func (a *testApp) createAssessment(assessmentType, crn string) string {
	response := a.commands(obj{
		"type": "CreateAssessmentCommand", "user": user(), "formVersion": "1", "assessmentType": assessmentType,
		"identifiers": obj{"CRN": crn}, "properties": obj{"status": single("NEW")},
	})
	return at(response, "commands", 0, "result", "assessmentUuid").(string)
}

func (a *testApp) answer(assessment, code, value string) {
	a.commands(obj{
		"type": "UpdateAssessmentAnswersCommand", "user": user(), "assessmentUuid": assessment,
		"added": obj{code: single(value)}, "removed": []string{},
	})
}

func (a *testApp) version(assessment string) any { return a.versionAt(assessment, nil) }

func (a *testApp) versionAt(assessment string, timestamp any) any {
	return a.query(obj{
		"type": "AssessmentVersionQuery", "user": user(),
		"assessmentIdentifier": obj{"type": "UUID", "uuid": assessment}, "timestamp": timestamp,
	})
}

type obj = map[string]any

func user() obj { return obj{"id": "test-user", "name": "Test User", "authSource": "HMPPS_AUTH"} }

func single(value string) obj { return obj{"type": "Single", "value": value} }

// at navigates decoded JSON by object keys and array indexes, giving nil for anything missing.
func at(value any, path ...any) any {
	for _, step := range path {
		switch key := step.(type) {
		case string:
			object, ok := value.(map[string]any)
			if !ok {
				return nil
			}
			value = object[key]
		case int:
			array, ok := value.([]any)
			if !ok || key >= len(array) {
				return nil
			}
			value = array[key]
		}
	}
	return value
}

func asJSON(value any) string {
	encoded, _ := json.Marshal(value)
	return string(encoded)
}

// tick waits long enough that the next request gets a later timestamp.
func tick() { time.Sleep(20 * time.Millisecond) }
