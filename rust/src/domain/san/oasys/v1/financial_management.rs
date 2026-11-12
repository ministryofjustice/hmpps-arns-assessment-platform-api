use super::PractitionerAnalysis;
use crate::{
    domain::san::oasys::{
        FieldsToMap, Mapped, NULL,
        answers::AnswersProvider,
        codes::{Field, Value},
        fields, text, when_value,
    },
    error::Result,
};

const ANALYSIS: PractitionerAnalysis = PractitionerAnalysis("FINANCE");

pub fn fields(_: &AnswersProvider) -> Result<FieldsToMap> {
    Ok(fields![
        "o5-3" => q3,
        "o5-4" => q4,
        "o5-5" => q5,
        "o5-6" => q6,
        "o5-97" => |ap| ANALYSIS.notes(ap),
        "o5-98" => |ap| ANALYSIS.risk_of_serious_harm(ap),
        "o5-99" => |ap| ANALYSIS.risk_of_reoffending(ap),
        "o5_SAN_STRENGTH" => |ap| ANALYSIS.strengths_or_protective_factors(ap),
    ])
}

fn q3(ap: &AnswersProvider) -> Mapped {
    Ok(when_value!(ap, ap.answer(Field::FINANCE_MONEY_MANAGEMENT)?.value()?, {
        GOOD | FAIRLY_GOOD => text("0"),
        FAIRLY_BAD => text("1"),
        BAD => text("2"),
        _ => NULL,
    }))
}

fn q4(ap: &AnswersProvider) -> Mapped {
    let Some(income) = ap.answer(Field::FINANCE_INCOME)?.values()? else {
        return Ok(NULL);
    };

    Ok(if ap.contains(&income, Value::OFFENDING)? {
        if income.len() == 1 { text("2") } else { text("1") }
    } else if ap.contains(&income, Value::UNKNOWN)? {
        text("M")
    } else {
        text("0")
    })
}

fn q5(ap: &AnswersProvider) -> Mapped {
    let Some(income) = ap.answer(Field::FINANCE_INCOME)?.values()? else {
        return Ok(NULL);
    };

    Ok(if ap.contains(&income, Value::FAMILY_OR_FRIENDS)? {
        when_value!(ap, ap.answer(Field::FAMILY_OR_FRIENDS_DETAILS)?.value()?, {
            YES => text("2"),
            UNKNOWN => text("M"),
            _ => text("0"),
        })
    } else {
        text("0")
    })
}

fn q6(ap: &AnswersProvider) -> Mapped {
    Ok(when_value!(ap, ap.answer(Field::FINANCE_MONEY_MANAGEMENT)?.value()?, {
        GOOD | FAIRLY_GOOD => text("0"),
        _ => NULL,
    }))
}
