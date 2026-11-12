use serde_json::Value as JsonValue;

use super::PractitionerAnalysis;
use crate::{
    domain::san::oasys::{FieldsToMap, Mapped, NULL, answers::AnswersProvider, codes::Field, fields, text, when_value},
    error::Result,
};

const ANALYSIS: PractitionerAnalysis = PractitionerAnalysis("ALCOHOL_USE");

pub fn fields(_: &AnswersProvider) -> Result<FieldsToMap> {
    Ok(fields![
        "o9-1" => q1,
        "o9-1-t" => q1t,
        "o9-2" => q2,
        "o9-97" => |ap| ANALYSIS.notes(ap),
        "o9-98" => |ap| ANALYSIS.risk_of_serious_harm(ap),
        "o9-99" => |ap| ANALYSIS.risk_of_reoffending(ap),
        "o9_SAN_STRENGTH" => |ap| ANALYSIS.strengths_or_protective_factors(ap),
    ])
}

fn q1(ap: &AnswersProvider) -> Mapped {
    Ok(when_value!(ap, ap.answer(Field::ALCOHOL_USE)?.value()?, {
        YES_WITHIN_LAST_THREE_MONTHS => {
            let total = frequency_score(ap, 0)?;
            let total = units_score(ap, total)?;
            oasys_score(total)
        },
        NO | YES_NOT_IN_LAST_THREE_MONTHS => text("0"),
        _ => NULL,
    }))
}

fn oasys_score(total: i32) -> JsonValue {
    match total {
        8.. => text("2"),
        5..=7 => text("1"),
        0..=4 => text("0"),
        _ => NULL,
    }
}

fn units_score(ap: &AnswersProvider, total: i32) -> Result<i32> {
    Ok(when_value!(ap, ap.answer(Field::ALCOHOL_UNITS)?.value()?, {
        UNITS_3_TO_4 => total + 1,
        UNITS_5_TO_6 => total + 2,
        UNITS_7_TO_9 => total + 3,
        UNITS_10_OR_MORE => total + 4,
        _ => total,
    }))
}

fn frequency_score(ap: &AnswersProvider, total: i32) -> Result<i32> {
    Ok(when_value!(ap, ap.answer(Field::ALCOHOL_FREQUENCY)?.value()?, {
        MULTIPLE_TIMES_A_MONTH => total + 1,
        LESS_THAN_4_TIMES_A_WEEK => total + 3,
        MORE_THAN_4_TIMES_A_WEEK => total + 4,
        _ => total,
    }))
}

fn q1t(ap: &AnswersProvider) -> Mapped {
    Ok(when_value!(ap, ap.answer(Field::ALCOHOL_USE)?.value()?, {
        YES_WITHIN_LAST_THREE_MONTHS => {
            let frequency = when_value!(ap, ap.answer(Field::ALCOHOL_FREQUENCY)?.value()?, {
                ONCE_A_MONTH_OR_LESS => Some("Only drinks once a month or less"),
                MULTIPLE_TIMES_A_MONTH => Some("Drinks multiple times a month"),
                LESS_THAN_4_TIMES_A_WEEK => Some("Drinks less than four times a week"),
                MORE_THAN_4_TIMES_A_WEEK => Some("Drinks more than four times a week"),
                _ => None,
            });
            let units = when_value!(ap, ap.answer(Field::ALCOHOL_UNITS)?.value()?, {
                UNITS_1_TO_2 => Some("consumes 1 to 2 units a day, when they drink"),
                UNITS_3_TO_4 => Some("consumes 3 to 4 units a day, when they drink"),
                UNITS_5_TO_6 => Some("consumes 5 to 6 units a day, when they drink"),
                UNITS_7_TO_9 => Some("consumes 7 to 9 units a day, when they drink"),
                UNITS_10_OR_MORE => Some("consumes 10 or more units a day, when they drink"),
                _ => None,
            });
            match (frequency, units) {
                (Some(frequency), Some(units)) => text(&format!("{frequency} and {units}.")),
                _ => NULL,
            }
        },
        _ => NULL,
    }))
}

fn q2(ap: &AnswersProvider) -> Mapped {
    Ok(when_value!(ap, ap.answer(Field::ALCOHOL_USE)?.value()?, {
        YES_WITHIN_LAST_THREE_MONTHS | YES_NOT_IN_LAST_THREE_MONTHS => {
            when_value!(ap, ap.answer(Field::ALCOHOL_EVIDENCE_OF_EXCESS_DRINKING)?.value()?, {
                NO_EVIDENCE => text("0"),
                YES_WITH_SOME_EVIDENCE => text("1"),
                YES_WITH_EVIDENCE => text("2"),
                _ => NULL,
            })
        },
        NO => text("0"),
        _ => NULL,
    }))
}
