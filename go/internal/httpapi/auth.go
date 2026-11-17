package httpapi

import (
	"context"
	"errors"
	"net/http"
	"slices"
	"strings"
	"time"

	"github.com/MicahParks/keyfunc/v3"
	"github.com/golang-jwt/jwt/v5"
	"golang.org/x/time/rate"
)

// Matches Spring Security's default allowance for clock skew.
const clockSkew = 60 * time.Second

var (
	commandRoles      = []string{"ROLE_AAP__FRONTEND_RW", "ROLE_AAP__COORDINATOR_RW", "ROLE_SENTENCE_PLAN_WRITE"}
	queryRoles        = []string{"ROLE_AAP__FRONTEND_RW", "ROLE_AAP__COORDINATOR_RW", "ROLE_MPOP__FRONTEND_RO", "ROLE_AAP__VIEW_R"}
	deleteRoles       = []string{"ROLE_AAP__COORDINATOR_RW", "ROLE_SENTENCE_PLAN_WRITE"}
	rebuildRoles      = []string{"ROLE_AAP__COORDINATOR_RW", "ROLE_SENTENCE_PLAN_WRITE", "ROLE_AAP_DATA_DELETION"}
	dataDeletionRoles = []string{"ROLE_AAP_DATA_DELETION"}
	planRoles         = []string{"ROLE_AAP__FRONTEND_RW", "ROLE_AAP__COORDINATOR_RW"}
	sarRoles          = []string{"ROLE_SAR_DATA_ACCESS"}
)

type authError struct {
	status           int
	message          string
	developerMessage string
}

func (e *authError) Error() string { return e.message }

func (e *authError) userMessage() string {
	if e.status == http.StatusForbidden {
		return "Forbidden: " + e.message
	}
	return "Unauthorised: " + e.message
}

var (
	errMissingToken = &authError{http.StatusUnauthorized, "Missing token", "Missing token"}
	errInvalidToken = &authError{http.StatusUnauthorized, "Invalid token", "Invalid token"}
	errExpiredToken = &authError{http.StatusUnauthorized, "Token expired", "Token expired"}
)

// Claims are the parts of an HMPPS Auth token the service uses.
type Claims struct {
	jwt.RegisteredClaims
	UserName string `json:"user_name"`
	ClientID string `json:"client_id"`
	// Authorities are HMPPS Auth roles, such as ROLE_AAP__FRONTEND_RW.
	Authorities []string `json:"authorities"`
}

// Principal is who made the request: the user for user tokens, otherwise the client.
func (c *Claims) Principal() string {
	for _, candidate := range []string{c.UserName, c.ClientID, c.Subject} {
		if candidate != "" {
			return candidate
		}
	}
	return ""
}

// authenticator checks bearer tokens against HMPPS Auth's signing keys, which are refreshed
// every five minutes, and early when a token is signed by an unknown key.
type authenticator struct {
	keys keyfunc.Keyfunc
}

func newAuthenticator(ctx context.Context, hmppsAuthURL string) (*authenticator, error) {
	keys, err := keyfunc.NewDefaultOverrideCtx(ctx, []string{strings.TrimRight(hmppsAuthURL, "/") + "/.well-known/jwks.json"}, keyfunc.Override{
		RefreshInterval:   5 * time.Minute,
		RefreshUnknownKID: rate.NewLimiter(rate.Every(30*time.Second), 1),
		HTTPTimeout:       10 * time.Second,
	})
	if err != nil {
		return nil, err
	}
	return &authenticator{keys: keys}, nil
}

// authorise checks the request's token, and that it has at least one of the roles.
func (a *authenticator) authorise(r *http.Request, roles []string) (*Claims, error) {
	header := r.Header.Get("Authorization")
	token, hasBearer := strings.CutPrefix(header, "Bearer ")
	if !hasBearer || token == "" {
		return nil, errMissingToken
	}

	claims := &Claims{}
	_, err := jwt.ParseWithClaims(token, claims, a.keys.KeyfuncCtx(r.Context()),
		jwt.WithValidMethods([]string{"RS256", "RS384", "RS512", "PS256", "PS384", "PS512"}),
		jwt.WithLeeway(clockSkew),
		jwt.WithExpirationRequired(),
	)
	switch {
	case errors.Is(err, jwt.ErrTokenExpired):
		return nil, errExpiredToken
	case err != nil:
		return nil, errInvalidToken
	}

	if !slices.ContainsFunc(claims.Authorities, func(authority string) bool { return slices.Contains(roles, authority) }) {
		return nil, &authError{http.StatusForbidden, "Access Denied", "Requires one of the roles: " + strings.Join(roles, ", ")}
	}
	return claims, nil
}
