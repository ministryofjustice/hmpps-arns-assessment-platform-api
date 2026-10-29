use super::PractitionerAnalysis;
use crate::{
    domain::san::oasys::{FieldsToMap, Mapped, NULL, answers::AnswersProvider, codes::Field, fields, text, when_value},
    error::Result,
};

const ANALYSIS: PractitionerAnalysis = PractitionerAnalysis("ACCOMMODATION");

pub fn fields(_: &AnswersProvider) -> Result<FieldsToMap> {
    Ok(fields![
        "o3-3" => q3,
        "o3-4" => q4,
        "o3-5" => q5,
        "o3-6" => q6,
        "o3-97" => |ap| ANALYSIS.notes(ap),
        "o3-98" => |ap| ANALYSIS.risk_of_serious_harm(ap),
        "o3-99" => |ap| ANALYSIS.risk_of_reoffending(ap),
        "o3_SAN_STRENGTH" => |ap| ANALYSIS.strengths_or_protective_factors(ap),
    ])
}

fn q3(ap: &AnswersProvider) -> Mapped {
    Ok(when_value!(ap, ap.answer(Field::CURRENT_ACCOMMODATION)?.value()?, {
        NO_ACCOMMODATION => text("YES"),
        TEMPORARY | SETTLED => text("NO"),
        _ => NULL,
    }))
}

fn q4(ap: &AnswersProvider) -> Mapped {
    let current = ap.answer(Field::CURRENT_ACCOMMODATION)?.value()?;
    if ap.is(&current, crate::domain::san::oasys::codes::Value::NO_ACCOMMODATION)? {
        return Ok(text("2"));
    }
    Ok(when_value!(ap, ap.answer(Field::SUITABLE_HOUSING)?.value()?, {
        YES => text("0"),
        YES_WITH_CONCERNS => text("1"),
        NO => text("2"),
        _ => NULL,
    }))
}

fn q5(ap: &AnswersProvider) -> Mapped {
    Ok(when_value!(ap, ap.answer(Field::CURRENT_ACCOMMODATION)?.value()?, {
        SETTLED => text("0"),
        NO_ACCOMMODATION => text("2"),
        TEMPORARY => when_value!(ap, ap.answer(Field::TYPE_OF_TEMPORARY_ACCOMMODATION)?.value()?, {
            SHORT_TERM => text("2"),
            _ => NULL,
        }),
        _ => NULL,
    }))
}

fn q6(ap: &AnswersProvider) -> Mapped {
    Ok(when_value!(ap, ap.answer(Field::CURRENT_ACCOMMODATION)?.value()?, {
        NO_ACCOMMODATION => text("2"),
        _ => when_value!(ap, ap.answer(Field::SUITABLE_HOUSING_LOCATION)?.value()?, {
            YES => text("0"),
            NO => text("2"),
            _ => NULL,
        }),
    }))
}
