use super::{PractitionerAnalysis, yes_no};
use crate::{
    domain::san::oasys::{
        FieldsToMap, Mapped, NULL,
        answers::AnswersProvider,
        codes::{Field, Value},
        fields, text, when_value,
    },
    error::Result,
};

const ANALYSIS: PractitionerAnalysis = PractitionerAnalysis("PERSONAL_RELATIONSHIPS_COMMUNITY");

pub fn fields(_: &AnswersProvider) -> Result<FieldsToMap> {
    Ok(fields![
        "o6-1" => q1,
        "o6-3" => q3,
        "o6-4" => q4,
        "o6-6" => q6,
        "o6-7da" => q7da,
        "o6-7-1-1da" => q711da,
        "o6-7-1-2da" => q712da,
        "o6-7-2-1da" => q721da,
        "o6-7-2-2da" => q722da,
        "o6-8" => q8,
        "o6-9" => q9,
        "o6-10" => q10,
        "o6-11" => |ap| yes_no(ap, Field::THINKING_BEHAVIOURS_ATTITUDES_RISK_SEXUAL_HARM),
        "o6-12" => q12,
        "o6-97" => |ap| ANALYSIS.notes(ap),
        "o6-98" => |ap| ANALYSIS.risk_of_serious_harm(ap),
        "o6-99" => |ap| ANALYSIS.risk_of_reoffending(ap),
        "o6_SAN_STRENGTH" => |ap| ANALYSIS.strengths_or_protective_factors(ap),
    ])
}

fn q1(ap: &AnswersProvider) -> Mapped {
    Ok(
        match ap
            .answer(Field::PERSONAL_RELATIONSHIPS_COMMUNITY_FAMILY_RELATIONSHIP)?
            .value()?
        {
            None => NULL,
            answered => when_value!(ap, answered, {
                UNSTABLE_RELATIONSHIP => text("2"),
                MIXED_RELATIONSHIP => text("1"),
                STABLE_RELATIONSHIP => text("0"),
                _ => text("M"),
            }),
        },
    )
}

fn q3(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::PERSONAL_RELATIONSHIPS_COMMUNITY_CHILDHOOD)?.value()?, {
            POSITIVE_CHILDHOOD => text("0"),
            MIXED_CHILDHOOD => text("1"),
            NEGATIVE_CHILDHOOD => text("2"),
            _ => NULL,
        }),
    )
}

fn q4(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::PERSONAL_RELATIONSHIPS_COMMUNITY_CURRENT_RELATIONSHIP)?.value()?, {
            HAPPY_RELATIONSHIP => text("0"),
            CONCERNS_HAPPY_RELATIONSHIP => text("1"),
            UNHAPPY_RELATIONSHIP => text("2"),
            _ => NULL,
        }),
    )
}

fn q6(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::PERSONAL_RELATIONSHIPS_COMMUNITY_INTIMATE_RELATIONSHIP)?.value()?, {
            STABLE_RELATIONSHIPS => text("0"),
            POSITIVE_AND_NEGATIVE_RELATIONSHIPS => text("1"),
            UNSTABLE_RELATIONSHIPS => text("2"),
            _ => NULL,
        }),
    )
}

fn q7da(ap: &AnswersProvider) -> Mapped {
    let perpetrator = yes_no(ap, Field::OFFENCE_ANALYSIS_PERPETRATOR_OF_DOMESTIC_ABUSE)?;
    let victim = yes_no(ap, Field::OFFENCE_ANALYSIS_VICTIM_OF_DOMESTIC_ABUSE)?;

    Ok(if perpetrator == "YES" || victim == "YES" {
        text("YES")
    } else if perpetrator == "NO" && victim == "NO" {
        text("NO")
    } else {
        NULL
    })
}

/// Whether domestic abuse involved the given relationship, when there was domestic abuse.
fn domestic_abuse_by_relationship(
    ap: &AnswersProvider,
    abuse: Field,
    abuse_type: Field,
    included: Value,
    excluded: Value,
) -> Mapped {
    Ok(when_value!(ap, ap.answer(abuse)?.value()?, {
        YES => {
            let abuse_type = ap.answer(abuse_type)?.value()?;
            if ap.is(&abuse_type, included)? || ap.is(&abuse_type, Value::FAMILY_MEMBER_AND_INTIMATE_PARTNER)? {
                text("YES")
            } else if ap.is(&abuse_type, excluded)? {
                text("NO")
            } else {
                NULL
            }
        },
        NO => text("NO"),
        _ => NULL,
    }))
}

fn q711da(ap: &AnswersProvider) -> Mapped {
    domestic_abuse_by_relationship(
        ap,
        Field::OFFENCE_ANALYSIS_VICTIM_OF_DOMESTIC_ABUSE,
        Field::OFFENCE_ANALYSIS_VICTIM_OF_DOMESTIC_ABUSE_TYPE,
        Value::INTIMATE_PARTNER,
        Value::FAMILY_MEMBER,
    )
}

fn q712da(ap: &AnswersProvider) -> Mapped {
    domestic_abuse_by_relationship(
        ap,
        Field::OFFENCE_ANALYSIS_VICTIM_OF_DOMESTIC_ABUSE,
        Field::OFFENCE_ANALYSIS_VICTIM_OF_DOMESTIC_ABUSE_TYPE,
        Value::FAMILY_MEMBER,
        Value::INTIMATE_PARTNER,
    )
}

fn q721da(ap: &AnswersProvider) -> Mapped {
    domestic_abuse_by_relationship(
        ap,
        Field::OFFENCE_ANALYSIS_PERPETRATOR_OF_DOMESTIC_ABUSE,
        Field::OFFENCE_ANALYSIS_PERPETRATOR_OF_DOMESTIC_ABUSE_TYPE,
        Value::INTIMATE_PARTNER,
        Value::FAMILY_MEMBER,
    )
}

fn q722da(ap: &AnswersProvider) -> Mapped {
    domestic_abuse_by_relationship(
        ap,
        Field::OFFENCE_ANALYSIS_PERPETRATOR_OF_DOMESTIC_ABUSE,
        Field::OFFENCE_ANALYSIS_PERPETRATOR_OF_DOMESTIC_ABUSE_TYPE,
        Value::FAMILY_MEMBER,
        Value::INTIMATE_PARTNER,
    )
}

fn q8(ap: &AnswersProvider) -> Mapped {
    let lives_with_partner = match ap.answer(Field::LIVING_WITH)?.values()? {
        Some(living_with) => ap.contains(&living_with, Value::PARTNER)?,
        None => false,
    };
    if lives_with_partner {
        return Ok(text("1"));
    }

    let has_partner = match ap
        .answer(Field::PERSONAL_RELATIONSHIPS_COMMUNITY_IMPORTANT_PEOPLE)?
        .values()?
    {
        Some(important_people) => ap.contains(&important_people, Value::PARTNER_INTIMATE_RELATIONSHIP)?,
        None => false,
    };
    Ok(if has_partner { text("2") } else { text("3") })
}

fn q9(ap: &AnswersProvider) -> Mapped {
    Ok(
        match ap
            .answer(Field::PERSONAL_RELATIONSHIPS_COMMUNITY_IMPORTANT_PEOPLE)?
            .values()?
        {
            Some(important_people) if ap.contains(&important_people, Value::CHILD_PARENTAL_RESPONSIBILITIES)? => {
                text("YES")
            }
            Some(_) => text("NO"),
            None => NULL,
        },
    )
}

fn q10(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::PERSONAL_RELATIONSHIPS_COMMUNITY_PARENTAL_RESPONSIBILITIES)?.value()?, {
            NO => text("Significantproblems"),
            SOMETIMES => text("Someproblems"),
            YES => text("Noproblems"),
            _ => NULL,
        }),
    )
}

fn q12(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::THINKING_BEHAVIOURS_ATTITUDES_EMOTIONAL_INTIMACY)?.value()?, {
            YES => text("2"),
            SOMETIMES => text("1"),
            NO => text("0"),
            _ => NULL,
        }),
    )
}
