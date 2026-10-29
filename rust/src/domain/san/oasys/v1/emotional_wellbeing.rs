use super::{PractitionerAnalysis, yes_no};
use crate::{
    domain::san::oasys::{FieldsToMap, Mapped, NULL, answers::AnswersProvider, codes::Field, fields, text, when_value},
    error::Result,
};

const ANALYSIS: PractitionerAnalysis = PractitionerAnalysis("HEALTH_WELLBEING");

pub fn fields(_: &AnswersProvider) -> Result<FieldsToMap> {
    Ok(fields![
        "o10-1" => q1,
        "o10-2" => mental_health_score,
        "o10-4" => q4,
        "o10-5" => q5,
        "o10-6" => mental_health_score,
        "o10-7_V2_CHILDHOOD" => |ap| yes_no(ap, Field::PERSONAL_RELATIONSHIPS_COMMUNITY_CHILDHOOD_BEHAVIOUR),
        "o10-7_V2_HISTHEADINJ" => |ap| yes_no(ap, Field::HEALTH_WELLBEING_HEAD_INJURY_OR_ILLNESS),
        "o10-7_V2_HISTPSYCH" => history_of_psychiatric_treatment,
        "o10-7_V2_PSYCHTREAT" => current_psychiatric_treatment,
        "o10-97" => |ap| ANALYSIS.notes(ap),
        "o10-98" => |ap| ANALYSIS.risk_of_serious_harm(ap),
        "o10-99" => |ap| ANALYSIS.risk_of_reoffending(ap),
        "o10_SAN_STRENGTH" => |ap| ANALYSIS.strengths_or_protective_factors(ap),
    ])
}

fn q1(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::HEALTH_WELLBEING_COPING_DAY_TO_DAY_LIFE)?.value()?, {
            YES => text("0"),
            YES_SOME_DIFFICULTIES => text("1"),
            NO => text("2"),
            _ => NULL,
        }),
    )
}

/// Questions 2 and 6 are both answered from the mental health condition.
fn mental_health_score(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::HEALTH_WELLBEING_MENTAL_HEALTH_CONDITION)?.value()?, {
            YES_ONGOING_SEVERE => text("2"),
            YES_ONGOING | YES_IN_THE_PAST => text("1"),
            NO => text("0"),
            _ => NULL,
        }),
    )
}

fn q4(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::HEALTH_WELLBEING_ATTITUDE_TOWARDS_SELF)?.value()?, {
            POSITIVE => text("0"),
            SOME_NEGATIVE_ASPECTS => text("1"),
            NEGATIVE => text("2"),
            _ => NULL,
        }),
    )
}

fn q5(ap: &AnswersProvider) -> Mapped {
    let answers = [
        yes_no(ap, Field::HEALTH_WELLBEING_ATTEMPTED_SUICIDE_OR_SUICIDAL_THOUGHTS)?,
        yes_no(ap, Field::HEALTH_WELLBEING_SELF_HARMED)?,
    ];

    Ok(if answers.iter().any(|answer| answer == "YES") {
        text("YES")
    } else if answers.iter().any(|answer| answer == "NO") {
        text("NO")
    } else {
        NULL
    })
}

fn history_of_psychiatric_treatment(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::HEALTH_WELLBEING_MENTAL_HEALTH_CONDITION)?.value()?, {
            YES_ONGOING_SEVERE | YES_ONGOING | YES_IN_THE_PAST => text("YES"),
            NO | UNKNOWN => text("NO"),
            _ => NULL,
        }),
    )
}

fn current_psychiatric_treatment(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::HEALTH_WELLBEING_MENTAL_HEALTH_CONDITION)?.value()?, {
            NO | UNKNOWN => text("NO"),
            _ => when_value!(ap, ap.answer(Field::HEALTH_WELLBEING_PSYCHIATRIC_TREATMENT)?.value()?, {
                YES | PENDING_TREATMENT => text("YES"),
                NO | UNKNOWN => text("NO"),
                _ => NULL,
            }),
        }),
    )
}
