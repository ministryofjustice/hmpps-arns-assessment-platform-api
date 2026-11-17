package httpapi

import (
	"encoding/json"
	"errors"
	"fmt"
	"log/slog"
	"net/http"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
)

// ErrorResponse is the HMPPS standard error body.
type ErrorResponse struct {
	Status           int     `json:"status"`
	ErrorCode        *string `json:"errorCode"`
	UserMessage      string  `json:"userMessage"`
	DeveloperMessage string  `json:"developerMessage"`
	MoreInfo         *string `json:"moreInfo"`
}

func writeJSON(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(body); err != nil {
		slog.Error("Failed to write response", "error", err)
	}
}

func writeErrorResponse(w http.ResponseWriter, status int, userMessage, developerMessage string) {
	if status == http.StatusNoContent {
		w.WriteHeader(status)
		return
	}
	writeJSON(w, status, ErrorResponse{Status: status, UserMessage: userMessage, DeveloperMessage: developerMessage})
}

func writeError(w http.ResponseWriter, r *http.Request, err error) {
	var authErr *authError
	if errors.As(err, &authErr) {
		writeErrorResponse(w, authErr.status, authErr.userMessage(), authErr.developerMessage)
		return
	}
	status := apperr.Status(err)
	if apperr.IsExpected(err) {
		slog.DebugContext(r.Context(), err.Error(), "status", status, "developerMessage", apperr.DeveloperMessage(err))
		writeErrorResponse(w, status, err.Error(), apperr.DeveloperMessage(err))
		return
	}
	slog.ErrorContext(r.Context(), "Unexpected error", "error", err, "path", r.URL.Path)
	writeErrorResponse(w, status, fmt.Sprintf("Unexpected error: %s", err), err.Error())
}

func notFound(w http.ResponseWriter, r *http.Request) {
	message := fmt.Sprintf("No static resource %s.", r.URL.Path[1:])
	writeErrorResponse(w, http.StatusNotFound, "No resource found failure: "+message, message)
}
