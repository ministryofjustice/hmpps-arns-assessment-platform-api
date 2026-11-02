package datetime

import (
	"testing"
	"time"
)

func TestFormatsLikeJackson(t *testing.T) {
	cases := map[string]string{
		"2025-01-01T09:00:00":        "2025-01-01T09:00:00",
		"2025-01-01T09:00:00.120":    "2025-01-01T09:00:00.12",
		"2025-01-01T09:00:00.123456": "2025-01-01T09:00:00.123456",
		"2025-01-01T09:00":           "2025-01-01T09:00:00",
		"2025-01-01T09:00:00Z":       "2025-01-01T09:00:00",
	}
	for in, want := range cases {
		if got := MustParse(in).String(); got != want {
			t.Errorf("%s: got %s, want %s", in, got, want)
		}
	}
	if _, err := Parse("2025-01-01"); err == nil {
		t.Error("expected a date alone to be rejected")
	}
}

func TestRoundsToDatabasePrecision(t *testing.T) {
	if got := MustParse("2025-01-01T09:00:00.000000499").ToDatabasePrecision(); got != MustParse("2025-01-01T09:00:00") {
		t.Errorf("got %s", got)
	}
	if got := MustParse("2025-01-01T09:00:00.0000005").ToDatabasePrecision(); got != MustParse("2025-01-01T09:00:00.000001") {
		t.Errorf("got %s", got)
	}
	day, _ := ParseDate("2025-01-01")
	if got := day.EndOfDay().ToDatabasePrecision(); got != MustParse("2025-01-02T00:00:00") {
		t.Errorf("got %s", got)
	}
}

func TestConvertsUsingLondonTimeLikeJavaTime(t *testing.T) {
	cases := map[string]string{
		"2025-01-01T09:00:00": "2025-01-01T09:00:00Z", // GMT
		"2025-07-01T09:00:00": "2025-07-01T08:00:00Z", // BST
		"2025-03-30T01:30:00": "2025-03-30T01:30:00Z", // in the spring gap: moved forward to 02:30 BST
		"2025-10-26T01:30:00": "2025-10-26T00:30:00Z", // in the autumn overlap: the earlier, BST, wins
	}
	for in, want := range cases {
		if got := MustParse(in).Instant().Format(time.RFC3339); got != want {
			t.Errorf("%s: got %s, want %s", in, got, want)
		}
	}
	summer := MustParse("2025-07-01T09:00:00")
	if FromInstant(summer.Instant()) != summer {
		t.Error("expected a round trip")
	}
}

func TestMatchesJavaToString(t *testing.T) {
	cases := map[string]string{
		"2025-01-01T09:00:00":        "2025-01-01T09:00",
		"2025-01-01T09:00:05":        "2025-01-01T09:00:05",
		"2025-01-01T09:00:00.1":      "2025-01-01T09:00:00.100",
		"2025-01-01T09:00:00.123456": "2025-01-01T09:00:00.123456",
	}
	for in, want := range cases {
		if got := MustParse(in).JavaString(); got != want {
			t.Errorf("%s: got %s, want %s", in, got, want)
		}
	}
}
