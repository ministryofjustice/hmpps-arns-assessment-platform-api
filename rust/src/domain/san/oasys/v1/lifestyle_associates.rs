use super::PractitionerAnalysis;
use crate::{
    domain::san::oasys::{FieldsToMap, Mapped, NULL, answers::AnswersProvider, codes::Field, fields, text, when_value},
    error::Result,
};

const ANALYSIS: PractitionerAnalysis = PractitionerAnalysis("THINKING_BEHAVIOURS_ATTITUDES");

pub fn fields(_: &AnswersProvider) -> Result<FieldsToMap> {
    Ok(fields![
        "o7-2" => q2,
        "o7-3" => q3,
        "o7-4" => q4,
        "o7-5" => q5,
        "o7-97" => |ap| ANALYSIS.notes(ap),
        "o7-98" => |ap| ANALYSIS.risk_of_serious_harm(ap),
        "o7-99" => |ap| ANALYSIS.risk_of_reoffending(ap),
    ])
}

fn q2(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::THINKING_BEHAVIOURS_ATTITUDES_OFFENDING_ACTIVITIES)?.value()?, {
            NO_OFFENDING_ACTIVITIES => text("0"),
            SOMETIMES_OFFENDING_ACTIVITIES => text("1"),
            YES_OFFENDING_ACTIVITIES => text("2"),
            _ => NULL,
        }),
    )
}

fn q3(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::THINKING_BEHAVIOURS_ATTITUDES_PEER_PRESSURE)?.value()?, {
            YES => text("0"),
            SOME => text("1"),
            NO => text("2"),
            _ => NULL,
        }),
    )
}

fn q4(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::THINKING_BEHAVIOURS_ATTITUDES_MANIPULATIVE_PREDATORY_BEHAVIOUR)?.value()?, {
            YES => text("2"),
            SOME => text("1"),
            NO => text("0"),
            _ => NULL,
        }),
    )
}

fn q5(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::THINKING_BEHAVIOURS_ATTITUDES_STABLE_BEHAVIOUR)?.value()?, {
            YES => text("0"),
            SOMETIMES => text("1"),
            NO => text("2"),
            _ => NULL,
        }),
    )
}
