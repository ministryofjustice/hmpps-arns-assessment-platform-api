use super::PractitionerAnalysis;
use crate::{
    domain::san::oasys::{FieldsToMap, Mapped, NULL, answers::AnswersProvider, codes::Field, fields, text, when_value},
    error::Result,
};

const ANALYSIS: PractitionerAnalysis = PractitionerAnalysis("THINKING_BEHAVIOURS_ATTITUDES");

pub fn fields(_: &AnswersProvider) -> Result<FieldsToMap> {
    Ok(fields![
        "o11-2" => q2,
        "o11-3" => q3,
        "o11-4" => q4,
        "o11-6" => q6,
        "o11-7" => q7,
        "o11-9" => q9,
        "o11-11" => q11,
        "o11-12" => q12,
        "o11-97" => |ap| ANALYSIS.notes(ap),
        "o11-98" => |ap| ANALYSIS.risk_of_serious_harm(ap),
        "o11-99" => |ap| ANALYSIS.risk_of_reoffending(ap),
    ])
}

fn q2(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::THINKING_BEHAVIOURS_ATTITUDES_IMPULSIVE_BEHAVIOUR)?.value()?, {
            NO => text("0"),
            SOMETIMES => text("1"),
            YES => text("2"),
            _ => NULL,
        }),
    )
}

fn q3(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::THINKING_BEHAVIOURS_ATTITUDES_VIOLENCE_CONTROLLING_BEHAVIOUR)?.value()?, {
            NO_VIOLENCE => text("0"),
            SOMETIMES => text("1"),
            YES_VIOLENCE => text("2"),
            _ => NULL,
        }),
    )
}

fn q4(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::THINKING_BEHAVIOURS_ATTITUDES_TEMPER_MANAGEMENT)?.value()?, {
            YES => text("0"),
            SOMETIMES => text("1"),
            NO => text("2"),
            _ => NULL,
        }),
    )
}

fn q6(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::THINKING_BEHAVIOURS_ATTITUDES_PROBLEM_SOLVING)?.value()?, {
            YES => text("0"),
            LIMITED_PROBLEM_SOLVING => text("1"),
            NO => text("2"),
            _ => NULL,
        }),
    )
}

fn q7(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::THINKING_BEHAVIOURS_ATTITUDES_CONSEQUENCES)?.value()?, {
            YES => text("0"),
            SOMETIMES => text("1"),
            NO => text("2"),
            _ => NULL,
        }),
    )
}

fn q9(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::THINKING_BEHAVIOURS_ATTITUDES_PEOPLES_VIEWS)?.value()?, {
            YES => text("0"),
            SOMETIMES => text("1"),
            NO => text("2"),
            _ => NULL,
        }),
    )
}

fn q11(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::THINKING_BEHAVIOURS_ATTITUDES_SEXUAL_PREOCCUPATION)?.value()?, {
            YES => text("2"),
            SOMETIMES => text("1"),
            NO => text("0"),
            _ => NULL,
        }),
    )
}

fn q12(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::THINKING_BEHAVIOURS_ATTITUDES_OFFENCE_RELATED_SEXUAL_INTEREST)?.value()?, {
            YES_OFFENCE_RELATED_SEXUAL_INTEREST => text("2"),
            SOME_OFFENCE_RELATED_SEXUAL_INTEREST => text("1"),
            NO_OFFENCE_RELATED_SEXUAL_INTEREST => text("0"),
            _ => NULL,
        }),
    )
}
