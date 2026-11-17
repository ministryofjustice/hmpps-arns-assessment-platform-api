// Package datetime provides LocalDateTime, matching how the Kotlin service used
// java.time.LocalDateTime.
//
// The API exchanges timestamps without an offset, in the service's time zone (Europe/London in
// every deployed environment), while the database stores TIMESTAMPTZ. Conversions always go
// through FromInstant and Instant.
package datetime

import (
	"encoding/json"
	"fmt"
	"strings"
	"time"
	_ "time/tzdata" // so the zone is available in minimal images
)

var zone = mustLoad("Europe/London")

func mustLoad(name string) *time.Location {
	location, err := time.LoadLocation(name)
	if err != nil {
		panic(err)
	}
	return location
}

// SetZone sets the zone local date-times are interpreted in.
func SetZone(location *time.Location) { zone = location }

func Zone() *time.Location { return zone }

// LocalDateTime is a wall-clock date and time, held in a time.Time in UTC.
type LocalDateTime struct{ wall time.Time }

func Of(year int, month time.Month, day, hour, minute, second, nanosecond int) LocalDateTime {
	return LocalDateTime{time.Date(year, month, day, hour, minute, second, nanosecond, time.UTC)}
}

func FromInstant(instant time.Time) LocalDateTime {
	w := instant.In(zone)
	return Of(w.Year(), w.Month(), w.Day(), w.Hour(), w.Minute(), w.Second(), w.Nanosecond())
}

func Now() LocalDateTime { return FromInstant(time.Now()) }

// Instant resolves the local time the way java.time does: in an overlap the earlier offset wins,
// and a time inside a gap is moved forward by the length of the gap.
func (l LocalDateTime) Instant() time.Time {
	offsetAt := func(t time.Time) time.Duration {
		_, offset := t.In(zone).Zone()
		return time.Duration(offset) * time.Second
	}
	before, after := offsetAt(l.wall.Add(-24*time.Hour)), offsetAt(l.wall.Add(24*time.Hour))

	for _, offset := range []time.Duration{before, after} {
		candidate := l.wall.Add(-offset)
		if FromInstant(candidate) == l {
			return candidate.In(time.UTC)
		}
	}
	return l.wall.Add(-before).In(time.UTC)
}

// ToDatabasePrecision rounds to the microsecond, as PostgreSQL does when storing a timestamp.
func (l LocalDateTime) ToDatabasePrecision() LocalDateTime {
	return LocalDateTime{l.wall.Add(500 * time.Nanosecond).Truncate(time.Microsecond)}
}

func (l LocalDateTime) Before(other LocalDateTime) bool { return l.wall.Before(other.wall) }
func (l LocalDateTime) After(other LocalDateTime) bool  { return l.wall.After(other.wall) }
func (l LocalDateTime) Sub(other LocalDateTime) time.Duration {
	return l.wall.Sub(other.wall)
}
func (l LocalDateTime) Add(d time.Duration) LocalDateTime { return LocalDateTime{l.wall.Add(d)} }
func (l LocalDateTime) AddDays(days int) LocalDateTime {
	return LocalDateTime{l.wall.AddDate(0, 0, days)}
}
func (l LocalDateTime) IsZero() bool { return l.wall.IsZero() }

// String is ISO-8601 without an offset, with the fraction trimmed of trailing zeros, as Jackson
// wrote it.
func (l LocalDateTime) String() string {
	return l.wall.Format("2006-01-02T15:04:05.999999999")
}

// JavaString mirrors LocalDateTime.toString(), which drops zero seconds and prints the fraction
// in groups of three digits. Only used where the Kotlin service stored that representation.
func (l LocalDateTime) JavaString() string {
	out := l.wall.Format("2006-01-02T15:04")
	seconds, nanos := l.wall.Second(), l.wall.Nanosecond()
	if seconds == 0 && nanos == 0 {
		return out
	}
	out += fmt.Sprintf(":%02d", seconds)
	switch {
	case nanos == 0:
	case nanos%1_000_000 == 0:
		out += fmt.Sprintf(".%03d", nanos/1_000_000)
	case nanos%1_000 == 0:
		out += fmt.Sprintf(".%06d", nanos/1_000)
	default:
		out += fmt.Sprintf(".%09d", nanos)
	}
	return out
}

// Parse accepts what Jackson accepted: optional seconds and fraction, and a trailing Z, which is
// ignored.
func Parse(value string) (LocalDateTime, error) {
	trimmed := strings.TrimSuffix(value, "Z")
	for _, layout := range []string{"2006-01-02T15:04:05", "2006-01-02T15:04"} {
		if parsed, err := time.Parse(layout, trimmed); err == nil {
			return LocalDateTime{parsed}, nil
		}
	}
	return LocalDateTime{}, fmt.Errorf(
		"invalid date-time '%s', expected an ISO-8601 local date-time such as 2025-01-01T09:00:00", value)
}

func MustParse(value string) LocalDateTime {
	parsed, err := Parse(value)
	if err != nil {
		panic(err)
	}
	return parsed
}

func (l LocalDateTime) MarshalJSON() ([]byte, error) { return json.Marshal(l.String()) }

func (l *LocalDateTime) UnmarshalJSON(data []byte) error {
	var raw string
	if err := json.Unmarshal(data, &raw); err != nil {
		return fmt.Errorf("expected a date-time string")
	}
	parsed, err := Parse(raw)
	if err != nil {
		return err
	}
	*l = parsed
	return nil
}

// Date is a calendar date, written as 2006-01-02.
type Date struct{ day time.Time }

func ParseDate(value string) (Date, error) {
	parsed, err := time.Parse(time.DateOnly, value)
	if err != nil {
		return Date{}, fmt.Errorf("invalid date '%s', expected a date such as 2025-01-01", value)
	}
	return Date{parsed}, nil
}

func (d Date) String() string { return d.day.Format(time.DateOnly) }

// StartOfDay is LocalDate.atTime(LocalTime.MIN).
func (d Date) StartOfDay() LocalDateTime { return LocalDateTime{d.day} }

// EndOfDay is LocalDate.atTime(LocalTime.MAX).
func (d Date) EndOfDay() LocalDateTime {
	return LocalDateTime{d.day.Add(24*time.Hour - time.Nanosecond)}
}

func (d Date) MarshalJSON() ([]byte, error) { return json.Marshal(d.String()) }
