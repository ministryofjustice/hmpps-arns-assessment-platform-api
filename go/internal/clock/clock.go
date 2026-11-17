// Package clock provides the clock for a single request. Requests may be backdated with the
// backdateTo query parameter, which shifts the whole clock (including Now) back to that time.
package clock

import (
	"fmt"
	"time"

	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/apperr"
	"github.com/ministryofjustice/hmpps-arns-assessment-platform-api/go/internal/datetime"
)

type Clock struct {
	offset          time.Duration
	requestDateTime datetime.LocalDateTime
}

func System() Clock { return withOffset(0) }

// BackdatedTo starts the clock at the given time. It can't be moved forward.
func BackdatedTo(backdateTo datetime.LocalDateTime) (Clock, error) {
	target := backdateTo.ToDatabasePrecision()
	offset := target.Instant().Sub(time.Now())
	if offset > 0 {
		return Clock{}, apperr.InvalidBackdate(fmt.Sprintf(
			"Invalid backdateTo parameter '%s' - the clock cannot be moved forward", target))
	}
	return withOffset(offset), nil
}

func withOffset(offset time.Duration) Clock {
	return Clock{
		offset:          offset,
		requestDateTime: datetime.FromInstant(time.Now().Add(offset)).ToDatabasePrecision(),
	}
}

func (c Clock) Now() datetime.LocalDateTime {
	return datetime.FromInstant(time.Now().Add(c.offset)).ToDatabasePrecision()
}

// RequestDateTime is fixed for the request, so every event a request creates shares a timestamp.
func (c Clock) RequestDateTime() datetime.LocalDateTime { return c.requestDateTime }
