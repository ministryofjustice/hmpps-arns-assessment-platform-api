// Package apperr has the errors the API returns, each with a user message, a developer message
// and a status code: the equivalent of the Kotlin AssessmentPlatformException hierarchy.
package apperr

import (
	"errors"
	"fmt"
	"net/http"
	"strings"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgconn"
)

// Error is a failure that's reported to the caller.
type Error struct {
	Status           int
	Message          string
	DeveloperMessage string
}

func (e *Error) Error() string { return e.Message }

func newError(status int, message, developerMessage string) *Error {
	return &Error{Status: status, Message: message, DeveloperMessage: developerMessage}
}

// EventHandlingError wraps a failure applying an event to an aggregate.
type EventHandlingError struct {
	EventUUID   uuid.UUID
	EventName   string
	HandlerName string
	Cause       error
}

func (e *EventHandlingError) Error() string {
	return fmt.Sprintf("%s was unable to handle %s with UUID: %s", e.HandlerName, e.EventName, e.EventUUID)
}

func (e *EventHandlingError) Unwrap() error { return e.Cause }

func EventHandling(eventUUID uuid.UUID, eventName string, cause error) error {
	return &EventHandlingError{EventUUID: eventUUID, EventName: eventName, HandlerName: eventName + "Handler", Cause: cause}
}

// ErrOptimisticLock means a snapshot was changed by another transaction after it was read.
var ErrOptimisticLock = errors.New("optimistic lock failure")

// Status is the HTTP status for an error: 500 for anything unexpected.
func Status(err error) int {
	var eventHandling *EventHandlingError
	var platform *Error
	switch {
	case errors.As(err, &eventHandling):
		return http.StatusBadRequest
	case errors.As(err, &platform):
		return platform.Status
	default:
		return http.StatusInternalServerError
	}
}

// IsExpected reports whether the error is one the API deliberately returns, rather than a fault.
func IsExpected(err error) bool {
	var eventHandling *EventHandlingError
	var platform *Error
	return errors.As(err, &eventHandling) || errors.As(err, &platform)
}

func DeveloperMessage(err error) string {
	var eventHandling *EventHandlingError
	if errors.As(err, &eventHandling) {
		return DeveloperMessage(eventHandling.Cause)
	}
	var platform *Error
	if errors.As(err, &platform) {
		return platform.DeveloperMessage
	}
	return err.Error()
}

// IsConcurrencyConflict reports whether another request writing the same assessment caused the
// failure, so it's worth retrying from the start.
func IsConcurrencyConflict(err error) bool {
	var eventHandling *EventHandlingError
	if errors.As(err, &eventHandling) {
		return false
	}
	var pgError *pgconn.PgError
	return errors.Is(err, ErrOptimisticLock) || (errors.As(err, &pgError) && pgError.Code == "23505")
}

func Internal(format string, args ...any) error {
	return fmt.Errorf(format, args...)
}

func AnswerNotFound(fieldCode string, aggregateUUID uuid.UUID) error {
	return newError(http.StatusBadRequest, "Answer not found",
		fmt.Sprintf(`The answer "%s" could not be found on aggregate "%s"`, fieldCode, aggregateUUID))
}

func PropertyNotFound(propertyName string, aggregateUUID uuid.UUID) error {
	return newError(http.StatusBadRequest, "Property not found",
		fmt.Sprintf(`The property "%s" could not be found on aggregate "%s"`, propertyName, aggregateUUID))
}

func CollectionNotFound(collectionUUID, aggregateUUID uuid.UUID) error {
	return newError(http.StatusBadRequest, "Collection not found",
		fmt.Sprintf(`No collection found with the UUID "%s" on aggregate "%s"`, collectionUUID, aggregateUUID))
}

func CollectionItemNotFound(collectionItemUUID, aggregateUUID uuid.UUID) error {
	return newError(http.StatusBadRequest, "Collection item not found",
		fmt.Sprintf(`No collection item found with the UUID "%s" on aggregate "%s"`, collectionItemUUID, aggregateUUID))
}

func CollectionIndexOutOfBounds(index, size int) error {
	return newError(http.StatusBadRequest, "Collection index out of bounds", fmt.Sprintf("Index: %d, Size: %d", index, size))
}

func AssessmentNotFoundByUUID(uuid uuid.UUID) error {
	return newError(http.StatusNotFound, "Assessment not found", fmt.Sprintf("No assessment found for UUID: %s", uuid))
}

func AssessmentNotFoundByIdentifier(identifierType, identifier, assessmentType string) error {
	return newError(http.StatusNotFound, "Assessment not found",
		fmt.Sprintf("No assessment found for %s: %s and type: %s", identifierType, identifier, assessmentType))
}

func EventNotFound(uuid uuid.UUID) error {
	return newError(http.StatusNotFound, "Event not found", fmt.Sprintf("Event %s not found", uuid))
}

func TimelineNotFound(uuid uuid.UUID) error {
	return newError(http.StatusNotFound, "Timeline not found", fmt.Sprintf("Timeline %s not found", uuid))
}

func UserDetailsNotFound() error {
	return newError(http.StatusNotFound, "User not found", "User not found")
}

func InvalidTimestamp(timestamp fmt.Stringer, message string) error {
	return newError(http.StatusBadRequest, fmt.Sprintf("Invalid timestamp %s", timestamp), message)
}

func InvalidBackdate(message string) error {
	return newError(http.StatusBadRequest, "Invalid backdateTo parameter", message)
}

func UndeleteNotAtTail(assessmentUUID uuid.UUID, pointInTime fmt.Stringer) error {
	return newError(http.StatusConflict, "Unable to undelete", fmt.Sprintf(
		"Events deleted from %s for assessment %s are not at the end of the event stream, so they cannot be safely undeleted",
		pointInTime, assessmentUUID))
}

func PlaceholderNotFound(index int) error {
	return newError(http.StatusBadRequest, fmt.Sprintf("Placeholder value not found @%d", index),
		fmt.Sprintf("Null value found at index: %d", index))
}

func InvalidReference(raw string) error {
	return InvalidCommand(fmt.Sprintf(`Invalid reference "%s": expected a UUID or a placeholder such as @0`, raw))
}

func InvalidCommand(message string) error {
	return newError(http.StatusBadRequest, "Unable to process commands", message)
}

func InvalidQuery(message string) error {
	return newError(http.StatusBadRequest, "Unable to process queries", message)
}

func TimelineBadCriteria() error {
	return newError(http.StatusBadRequest, "Unable to execute timeline query",
		"Must specify at least one of assessmentUuid or userUuid")
}

func CollectionDepthOutOfBounds(depth int, collectionUUID uuid.UUID) error {
	return newError(http.StatusBadRequest, "Collection depth out of bounds",
		fmt.Sprintf(`Invalid depth %d for collection "%s"`, depth, collectionUUID))
}

// SubjectAccessRequestNoAssessments takes "TYPE=id" pairs.
func SubjectAccessRequestNoAssessments(identifiers []string) error {
	return newError(http.StatusNoContent, "There are no assessments",
		"There were no assessments found for the identifiers: "+strings.Join(identifiers, ", "))
}

func AssessmentNotPlan(assessmentUUID uuid.UUID) error {
	return newError(http.StatusBadRequest, "Assessment is not a Sentence Plan",
		fmt.Sprintf("Assessment with UUID: %s is not a Sentence Plan", assessmentUUID))
}

func UndefinedPosition(message string) error {
	return newError(http.StatusBadRequest, message, message)
}

// InvalidPayload is a request body or parameter that couldn't be read.
func InvalidPayload(detail string) error {
	return newError(http.StatusBadRequest, "Invalid payload: "+detail, detail)
}
