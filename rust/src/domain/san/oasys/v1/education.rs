use serde_json::Value as JsonValue;

use super::{PractitionerAnalysis, value_of, yes_no};
use crate::{
    domain::san::oasys::{
        FieldsToMap, Mapped, NULL,
        answers::AnswersProvider,
        codes::{Field, Value},
        fields, text, when_value,
    },
    error::Result,
};

const ANALYSIS: PractitionerAnalysis = PractitionerAnalysis("EMPLOYMENT_EDUCATION");

pub fn fields(_: &AnswersProvider) -> Result<FieldsToMap> {
    Ok(fields![
        "o4-2" => q2,
        "o4-3" => q3,
        "o4-4" => q4,
        "o4-7" => q7,
        "o4-7-1" => q71,
        "o4-8" => q8,
        "o4-9" => q9,
        "o4-10" => q10,
        "o4-94" => |ap| ANALYSIS.notes(ap),
        "o4-96" => |ap| ANALYSIS.risk_of_serious_harm(ap),
        "o4-98" => |ap| ANALYSIS.risk_of_reoffending(ap),
        "o4_SAN_STRENGTH" => |ap| ANALYSIS.strengths_or_protective_factors(ap),
        "oSC2" => |ap| yes_no(ap, Field::EDUCATION_PROFESSIONAL_OR_VOCATIONAL_QUALIFICATIONS),
        "oSC2-t" => |ap| value_of(ap, Field::EDUCATION_PROFESSIONAL_OR_VOCATIONAL_QUALIFICATIONS_YES_DETAILS),
        "oSC3" => q_sc3,
        "oSC4" => q_sc4,
        "oSC5" => q_sc5,
        "oSC8" => q_sc8,
    ])
}

fn q2(ap: &AnswersProvider) -> Mapped {
    Ok(when_value!(ap, ap.answer(Field::EMPLOYMENT_STATUS)?.value()?, {
        UNEMPLOYED_LOOKING_FOR_WORK | UNEMPLOYED_NOT_LOOKING_FOR_WORK => text("YES"),
        EMPLOYED | SELF_EMPLOYED => text("NO"),
        RETIRED | CURRENTLY_UNAVAILABLE_FOR_WORK => text("NA"),
        _ => NULL,
    }))
}

fn employment_history(ap: &AnswersProvider) -> Mapped {
    Ok(when_value!(ap, ap.answer(Field::EMPLOYMENT_HISTORY)?.value()?, {
        STABLE => text("0"),
        PERIODS_OF_INSTABILITY => text("1"),
        UNSTABLE => text("2"),
        UNKNOWN => text("M"),
        _ => NULL,
    }))
}

fn q3(ap: &AnswersProvider) -> Mapped {
    Ok(when_value!(ap, ap.answer(Field::EMPLOYMENT_STATUS)?.value()?, {
        UNEMPLOYED_NOT_LOOKING_FOR_WORK | UNEMPLOYED_LOOKING_FOR_WORK => {
            when_value!(ap, ap.answer(Field::HAS_BEEN_EMPLOYED)?.value()?, {
                NO => text("2"),
                _ => employment_history(ap)?,
            })
        },
        _ => employment_history(ap)?,
    }))
}

fn q4(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::EDUCATION_TRANSFERABLE_SKILLS)?.value()?, {
            NO => text("2"),
            YES_SOME_SKILLS => text("1"),
            YES => text("0"),
            _ => NULL,
        }),
    )
}

fn severity_of(ap: &AnswersProvider, field: Field) -> Result<Option<i32>> {
    Ok(when_value!(ap, ap.answer(field)?.value()?, {
        SIGNIFICANT_DIFFICULTIES => Some(2),
        SOME_DIFFICULTIES => Some(1),
        _ => None,
    }))
}

fn q7(ap: &AnswersProvider) -> Mapped {
    let difficulties = ap.answer(Field::EDUCATION_DIFFICULTIES)?.values()?.unwrap_or_default();

    // The categories' options are validated up front, as they were keys of a map in Kotlin.
    for category in [Value::READING, Value::WRITING, Value::NUMERACY] {
        ap.get(category)?;
    }
    let severities = [
        Field::EDUCATION_DIFFICULTIES_READING_SEVERITY,
        Field::EDUCATION_DIFFICULTIES_WRITING_SEVERITY,
        Field::EDUCATION_DIFFICULTIES_NUMERACY_SEVERITY,
    ];

    Ok(if ap.contains(&difficulties, Value::NONE)? {
        text("0")
    } else if !difficulties.is_empty() {
        let mut highest = None;
        for field in severities {
            highest = highest.max(severity_of(ap, field)?);
        }
        highest.map_or(NULL, |severity| text(&severity.to_string()))
    } else {
        NULL
    })
}

fn q71(ap: &AnswersProvider) -> Mapped {
    Ok(match ap.answer(Field::EDUCATION_DIFFICULTIES)?.values()? {
        None => NULL,
        Some(difficulties) if difficulties.is_empty() => NULL,
        Some(difficulties) if ap.contains(&difficulties, Value::NONE)? => NULL,
        Some(difficulties) => JsonValue::from(difficulties),
    })
}

fn q8(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::HEALTH_WELLBEING_LEARNING_DIFFICULTIES)?.value()?, {
            YES_SIGNIFICANT_DIFFICULTIES => text("2"),
            YES_SOME_DIFFICULTIES => text("1"),
            NO => text("0"),
            _ => NULL,
        }),
    )
}

fn q9(ap: &AnswersProvider) -> Mapped {
    let highest_level = ap.answer(Field::EDUCATION_HIGHEST_LEVEL_COMPLETED)?.value()?;
    let has_qualifications = |ap: &AnswersProvider| -> Mapped {
        Ok(
            when_value!(ap, ap.answer(Field::EDUCATION_PROFESSIONAL_OR_VOCATIONAL_QUALIFICATIONS)?.value()?, {
                YES => text("0"),
                _ => NULL,
            }),
        )
    };

    Ok(when_value!(ap, highest_level.clone(), {
        LEVEL_1 | ENTRY_LEVEL | NONE_OF_THESE => {
            when_value!(ap, ap.answer(Field::EDUCATION_PROFESSIONAL_OR_VOCATIONAL_QUALIFICATIONS)?.value()?, {
                YES => text("0"),
                NO => text("2"),
                _ => NULL,
            })
        },
        LEVEL_2 | LEVEL_3 | LEVEL_4 | LEVEL_5 | LEVEL_6 | LEVEL_7 | LEVEL_8 => text("0"),
        NOT_SURE => has_qualifications(ap)?,
        _ => if highest_level.is_none() { has_qualifications(ap)? } else { NULL },
    }))
}

fn q10(ap: &AnswersProvider) -> Mapped {
    Ok(when_value!(ap, ap.answer(Field::EDUCATION_EXPERIENCE)?.value()?, {
        POSITIVE | MOSTLY_POSITIVE => text("0"),
        POSITIVE_AND_NEGATIVE => text("1"),
        NEGATIVE | MOSTLY_NEGATIVE => text("2"),
        _ => NULL,
    }))
}

fn q_sc3(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::EDUCATION_HIGHEST_LEVEL_COMPLETED)?.value()?, {
            ENTRY_LEVEL => text("ANYOTHER"),
            LEVEL_1 | LEVEL_2 | LEVEL_3 | LEVEL_4 | LEVEL_5 | LEVEL_6 | LEVEL_7 | LEVEL_8 => text("MATHSENGLISH"),
            NONE_OF_THESE => text("NOQUAL"),
            _ => NULL,
        }),
    )
}

fn q_sc4(ap: &AnswersProvider) -> Mapped {
    Ok(when_value!(ap, ap.answer(Field::EMPLOYMENT_STATUS)?.value()?, {
        RETIRED => text("FULLTIME"),
        EMPLOYED => when_value!(ap, ap.answer(Field::EMPLOYMENT_TYPE)?.value()?, {
            FULL_TIME => text("FULLTIME"),
            PART_TIME | TEMPORARY_OR_CASUAL | APPRENTICESHIP => text("PARTTIME"),
            _ => NULL,
        }),
        CURRENTLY_UNAVAILABLE_FOR_WORK | UNEMPLOYED_LOOKING_FOR_WORK | UNEMPLOYED_NOT_LOOKING_FOR_WORK => {
            when_value!(ap, ap.answer(Field::HAS_BEEN_EMPLOYED)?.value()?, {
                NO => text("UNEMPLOYED"),
                _ => NULL,
            })
        },
        _ => NULL,
    }))
}

fn q_sc5(ap: &AnswersProvider) -> Mapped {
    Ok(when_value!(ap, ap.answer(Field::EMPLOYMENT_STATUS)?.value()?, {
        EMPLOYED | SELF_EMPLOYED => text("YES"),
        UNEMPLOYED_LOOKING_FOR_WORK | UNEMPLOYED_NOT_LOOKING_FOR_WORK => text("NO"),
        _ => NULL,
    }))
}

fn q_sc8(ap: &AnswersProvider) -> Mapped {
    Ok(when_value!(ap, ap.answer(Field::FINANCE_MONEY_MANAGEMENT)?.value()?, {
        GOOD => text("YES"),
        FAIRLY_GOOD => text("SOMETIMES"),
        FAIRLY_BAD | BAD => text("NOTCONFIDENT"),
        _ => NULL,
    }))
}
