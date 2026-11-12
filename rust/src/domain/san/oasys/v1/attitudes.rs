use super::PractitionerAnalysis;
use crate::{
    domain::san::oasys::{FieldsToMap, Mapped, NULL, answers::AnswersProvider, codes::Field, fields, text, when_value},
    error::Result,
};

const ANALYSIS: PractitionerAnalysis = PractitionerAnalysis("THINKING_BEHAVIOURS_ATTITUDES");

pub fn fields(_: &AnswersProvider) -> Result<FieldsToMap> {
    Ok(fields![
        "o12-1" => q1,
        "o12-3" => q3,
        "o12-4" => q4,
        "o12-9" => q9,
        "o12-97" => |ap| ANALYSIS.notes(ap),
        "o12-98" => |ap| ANALYSIS.risk_of_serious_harm(ap),
        "o12-99" => |ap| ANALYSIS.risk_of_reoffending(ap),
    ])
}

fn q1(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::THINKING_BEHAVIOURS_ATTITUDES_CRIMINAL_BEHAVIOUR)?.value()?, {
            NO => text("0"),
            SOMETIMES => text("1"),
            YES => text("2"),
            _ => NULL,
        }),
    )
}

fn q3(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::THINKING_BEHAVIOURS_ATTITUDES_POSITIVE_ATTITUDE)?.value()?, {
            YES_POSITIVE => text("0"),
            NEGATIVE_ATTITUDE_NO_CONCERNS => text("1"),
            NEGATIVE_ATTITUDE_AND_CONCERNS => text("2"),
            _ => NULL,
        }),
    )
}

fn q4(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::THINKING_BEHAVIOURS_ATTITUDES_SUPERVISION)?.value()?, {
            YES_SUPERVISION => text("0"),
            UNSURE_SUPERVISION => text("1"),
            NO_SUPERVISION => text("2"),
            _ => NULL,
        }),
    )
}

fn q9(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::THINKING_BEHAVIOURS_ATTITUDES_HOSTILE_ORIENTATION)?.value()?, {
            NO => text("0"),
            SOME => text("1"),
            YES => text("2"),
            _ => NULL,
        }),
    )
}
