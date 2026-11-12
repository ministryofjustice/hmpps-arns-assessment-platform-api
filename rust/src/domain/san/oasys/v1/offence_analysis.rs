use serde_json::json;

use super::{ticked, value_of};
use crate::{
    domain::san::oasys::{
        FieldsToMap, Mapped, NULL,
        answers::AnswersProvider,
        codes::{Collection, Field, Value},
        fields, mapping, optional_text, text, when_value,
    },
    error::Result,
    model::{self, Answers},
};

pub fn fields(ap: &AnswersProvider) -> Result<FieldsToMap> {
    use Field::*;

    let mut fields = fields![
        "o2-1" => |ap| value_of(ap, OFFENCE_ANALYSIS_DESCRIPTION_OF_OFFENCE),
        "o2-2_V2_WEAPON" => |ap| ticked(ap, OFFENCE_ANALYSIS_ELEMENTS, Value::WEAPON),
        "o2-2_V2_ANYVIOL" => |ap| ticked(ap, OFFENCE_ANALYSIS_ELEMENTS, Value::VIOLENCE_OR_COERCION),
        "o2-2_V2_ARSON" => |ap| ticked(ap, OFFENCE_ANALYSIS_ELEMENTS, Value::ARSON),
        "o2-2_V2_DOM_ABUSE" => |ap| ticked(ap, OFFENCE_ANALYSIS_ELEMENTS, Value::DOMESTIC_ABUSE),
        "o2-2_V2_EXCESSIVE" => |ap| ticked(ap, OFFENCE_ANALYSIS_ELEMENTS, Value::EXCESSIVE_OR_SADISTIC_VIOLENCE),
        "o2-2_V2_PHYSICALDAM" => |ap| ticked(ap, OFFENCE_ANALYSIS_ELEMENTS, Value::PHYSICAL_DAMAGE_TO_PROPERTY),
        "o2-2_V2_SEXUAL" => |ap| ticked(ap, OFFENCE_ANALYSIS_ELEMENTS, Value::SEXUAL_ELEMENT),
        "o2-3" => q3,
        "o2-6" => |ap| yes_no_if_answered(ap, OFFENCE_ANALYSIS_IMPACT_ON_VICTIMS),
        "o2-7" => q7,
        "o2-7-1" => q71,
        "o2-7-2" => |ap| ticked(ap, OFFENCE_ANALYSIS_MOTIVATIONS, Value::PRESSURISED_BY_OTHERS),
        "o2-7-3" => q73,
        "o2-8" => |ap| value_of(ap, OFFENCE_ANALYSIS_REASON),
        "o2-9_V2_SEXUAL" => |ap| ticked(ap, OFFENCE_ANALYSIS_MOTIVATIONS, Value::SEXUAL_MOTIVATION),
        "o2-9_V2_FINANCIAL" => |ap| ticked(ap, OFFENCE_ANALYSIS_MOTIVATIONS, Value::FINANCIAL_MOTIVATION),
        "o2-9_V2_ADDICTION" => |ap| ticked(ap, OFFENCE_ANALYSIS_MOTIVATIONS, Value::ADDICTIONS_OR_PERCEIVED_NEEDS),
        "o2-9_V2_EMOTIONAL" => |ap| ticked(ap, OFFENCE_ANALYSIS_MOTIVATIONS, Value::EMOTIONAL_STATE),
        "o2-9_V2_RACIAL" => |ap| ticked(ap, OFFENCE_ANALYSIS_MOTIVATIONS, Value::HATRED_OF_IDENTIFIABLE_GROUPS),
        "o2-9_V2_THRILL" => |ap| ticked(ap, OFFENCE_ANALYSIS_MOTIVATIONS, Value::THRILL_SEEKING),
        "o2-9_V2_OTHER" => |ap| ticked(ap, OFFENCE_ANALYSIS_MOTIVATIONS, Value::OTHER),
        "o2-9-t_V2" => |ap| value_of(ap, OFFENCE_ANALYSIS_MOTIVATIONS_OTHER_DETAILS),
        "o2-11" => |ap| yes_no_if_answered(ap, OFFENCE_ANALYSIS_ACCEPT_RESPONSIBILITY),
        "o2-11-t" => q11t,
        "o2-12" => |ap| value_of(ap, OFFENCE_ANALYSIS_PATTERNS_OF_OFFENDING),
        "o2-13" => |ap| yes_no_if_answered(ap, OFFENCE_ANALYSIS_ESCALATION),
        "o2-98" => q98,
        "o2-99" => |ap| yes_no_if_answered(ap, OFFENCE_ANALYSIS_RISK),
    ];
    fields.extend(victims(ap)?);
    Ok(fields)
}

/// One entry per victim, as `victim0`, `victim1`, ...
fn victims(ap: &AnswersProvider) -> Result<FieldsToMap> {
    Ok(ap
        .answer_collection(Collection::OFFENCE_ANALYSIS_VICTIM)?
        .collection()?
        .into_iter()
        .enumerate()
        .map(|(index, victim)| {
            let question = format!("victim{index}");
            let answer = mapping(move |ap| {
                Ok(json!({
                    "oAGE_OF_VICTIM_ELM": victim_age(ap, &victim)?,
                    "oGENDER_ELM": victim_gender(ap, &victim)?,
                    "oETHNIC_CATEGORY_ELM": victim_race(ap, &victim)?,
                    "oVICTIM_RELATION_ELM": victim_relationship(ap, &victim)?,
                }))
            });
            (question, answer)
        })
        .collect())
}

/// A victim's answer, with the field set as the context for option lookups.
fn victim_answer(ap: &AnswersProvider, victim: &Answers, field: Field) -> Option<String> {
    ap.set_context(field);
    match victim.get(&field.lower()) {
        Some(model::Value::Single { value }) => Some(value.clone()),
        _ => None,
    }
}

fn victim_age(ap: &AnswersProvider, victim: &Answers) -> Mapped {
    Ok(
        when_value!(ap, victim_answer(ap, victim, Field::OFFENCE_ANALYSIS_VICTIM_AGE), {
            AGE_0_TO_4_YEARS => text("0"),
            AGE_5_TO_11_YEARS => text("1"),
            AGE_12_TO_15_YEARS => text("2"),
            AGE_16_TO_17_YEARS => text("3"),
            AGE_18_TO_20_YEARS => text("4"),
            AGE_21_TO_25_YEARS => text("5"),
            AGE_26_TO_49_YEARS => text("6"),
            AGE_50_TO_64_YEARS => text("7"),
            AGE_65_AND_OVER => text("8"),
            _ => NULL,
        }),
    )
}

fn victim_gender(ap: &AnswersProvider, victim: &Answers) -> Mapped {
    Ok(
        when_value!(ap, victim_answer(ap, victim, Field::OFFENCE_ANALYSIS_VICTIM_SEX), {
            MALE => text("1"),
            FEMALE => text("2"),
            UNKNOWN => text("0"),
            _ => NULL,
        }),
    )
}

fn victim_race(ap: &AnswersProvider, victim: &Answers) -> Mapped {
    Ok(
        when_value!(ap, victim_answer(ap, victim, Field::OFFENCE_ANALYSIS_VICTIM_RACE), {
            WHITE_ENGLISH_WELSH_SCOTTISH_NORTHERN_IRISH_OR_BRITISH => text("W1"),
            WHITE_IRISH => text("W2"),
            WHITE_GYPSY_OR_IRISH_TRAVELLER => text("W4"),
            WHITE_ROMA => text("W5"),
            WHITE_ANY_OTHER_WHITE_BACKGROUND => text("W9"),
            MIXED_WHITE_AND_BLACK_CARIBBEAN => text("M1"),
            MIXED_WHITE_AND_BLACK_AFRICAN => text("M2"),
            MIXED_WHITE_AND_ASIAN => text("M3"),
            MIXED_ANY_OTHER_MIXED_OR_MULTIPLE_ETHNIC_BACKGROUND_BACKGROUND => text("M9"),
            ASIAN_OR_ASIAN_BRITISH_INDIAN => text("A1"),
            ASIAN_OR_ASIAN_BRITISH_PAKISTANI => text("A2"),
            ASIAN_OR_ASIAN_BRITISH_BANGLADESHI => text("A3"),
            ASIAN_OR_ASIAN_BRITISH_CHINESE => text("A4"),
            ASIAN_OR_ASIAN_BRITISH_ANY_OTHER_ASIAN_BACKGROUND => text("A9"),
            BLACK_OR_BLACK_BRITISH_CARIBBEAN => text("B1"),
            BLACK_OR_BLACK_BRITISH_AFRICAN => text("B2"),
            BLACK_OR_BLACK_BRITISH_ANY_OTHER_BLACK_BACKGROUND => text("B9"),
            ARAB => text("O2"),
            ANY_OTHER_ETHNIC_GROUP => text("O9"),
            UNKNOWN => text("NS"),
            _ => NULL,
        }),
    )
}

fn victim_relationship(ap: &AnswersProvider, victim: &Answers) -> Mapped {
    Ok(
        when_value!(ap, victim_answer(ap, victim, Field::OFFENCE_ANALYSIS_VICTIM_RELATIONSHIP), {
            STRANGER => text("0"),
            CRIMINAL_JUSTICE_STAFF => text("12"),
            POP_PARENT_OR_STEP_PARENT => text("14"),
            POP_EX_PARTNER => text("15"),
            POP_CHILD_OR_STEP_CHILD => text("5"),
            OTHER_FAMILY_MEMBER => text("6"),
            POP_PARTNER => text("1"),
            OTHER => text("13"),
            _ => NULL,
        }),
    )
}

/// `YES` or `NO`, without validating the options when the field is unanswered.
fn yes_no_if_answered(ap: &AnswersProvider, field: Field) -> Mapped {
    Ok(match ap.answer(field)?.value()? {
        None => NULL,
        answered => when_value!(ap, answered, {
            YES => text("YES"),
            NO => text("NO"),
            _ => NULL,
        }),
    })
}

fn q3(ap: &AnswersProvider) -> Mapped {
    let mut answers = Vec::new();
    for element in ap
        .answer(Field::OFFENCE_ANALYSIS_ELEMENTS)?
        .values()?
        .unwrap_or_default()
    {
        let mapped = when_value!(ap, Some(element), {
            VICTIM_TARGETED => Some("DIRECTCONT"),
            HATRED_OF_IDENTIFIABLE_GROUPS => Some("HATE"),
            _ => None,
        });
        answers.extend(mapped);
    }

    for victim in ap
        .answer_collection(Collection::OFFENCE_ANALYSIS_VICTIM)?
        .collection()?
    {
        let relationship = victim_answer(ap, &victim, Field::OFFENCE_ANALYSIS_VICTIM_RELATIONSHIP);
        if ap.is(&relationship, Value::STRANGER)? {
            answers.push("STRANGERS");
            break;
        }
    }

    Ok(if answers.is_empty() {
        NULL
    } else {
        text(&answers.join(","))
    })
}

fn q7(ap: &AnswersProvider) -> Mapped {
    Ok(match ap.answer(Field::OFFENCE_ANALYSIS_HOW_MANY_INVOLVED)?.value()? {
        None => NULL,
        involved => when_value!(ap, involved, {
            ONE | TWO | THREE | FOUR | FIVE | SIX_TO_10 | ELEVEN_TO_15 | MORE_THAN_15 => text("YES"),
            NONE => text("NO"),
            _ => NULL,
        }),
    })
}

fn q71(ap: &AnswersProvider) -> Mapped {
    Ok(match ap.answer(Field::OFFENCE_ANALYSIS_HOW_MANY_INVOLVED)?.value()? {
        None => NULL,
        involved => when_value!(ap, involved, {
            ONE => text("110"),
            TWO => text("120"),
            THREE => text("130"),
            FOUR => text("140"),
            FIVE => text("150"),
            SIX_TO_10 => text("160"),
            ELEVEN_TO_15 => text("170"),
            MORE_THAN_15 => text("180"),
            _ => NULL,
        }),
    })
}

fn q73(ap: &AnswersProvider) -> Mapped {
    let with_details = |label: &str, details: Option<String>| match details {
        Some(details) => text(&format!("{label} - {details}")),
        None => text(label),
    };

    Ok(when_value!(ap, ap.answer(Field::OFFENCE_ANALYSIS_LEADER)?.value()?, {
        YES => with_details("Yes", ap.answer(Field::OFFENCE_ANALYSIS_LEADER_YES_DETAILS)?.value()?),
        NO => with_details("No", ap.answer(Field::OFFENCE_ANALYSIS_LEADER_NO_DETAILS)?.value()?),
        _ => NULL,
    }))
}

fn q11t(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::OFFENCE_ANALYSIS_ACCEPT_RESPONSIBILITY)?.value()?, {
            YES => optional_text(ap.answer(Field::OFFENCE_ANALYSIS_ACCEPT_RESPONSIBILITY_YES_DETAILS)?.value()?),
            NO => optional_text(ap.answer(Field::OFFENCE_ANALYSIS_ACCEPT_RESPONSIBILITY_NO_DETAILS)?.value()?),
            _ => NULL,
        }),
    )
}

fn q98(ap: &AnswersProvider) -> Mapped {
    Ok(when_value!(ap, ap.answer(Field::OFFENCE_ANALYSIS_RISK)?.value()?, {
        YES => optional_text(ap.answer(Field::OFFENCE_ANALYSIS_RISK_YES_DETAILS)?.value()?),
        NO => optional_text(ap.answer(Field::OFFENCE_ANALYSIS_RISK_NO_DETAILS)?.value()?),
        _ => NULL,
    }))
}
