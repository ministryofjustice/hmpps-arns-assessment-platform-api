use super::{PractitionerAnalysis, value_of};
use crate::{
    domain::san::oasys::{
        FieldsToMap, Mapped, NULL,
        answers::AnswersProvider,
        codes::{Field, Value},
        fields, text, when_value,
    },
    error::Result,
};

const ANALYSIS: PractitionerAnalysis = PractitionerAnalysis("DRUG_USE");

pub fn fields(_: &AnswersProvider) -> Result<FieldsToMap> {
    use Field::*;

    Ok(fields![
        "o8-1" => q1,
        "o8-2-1-1" => |ap| usage_frequency_score(ap, HOW_OFTEN_USED_LAST_SIX_MONTHS_HEROIN),
        "o8-2-1-3" => |ap| yes_if_more_than_six(ap, DRUG_LAST_USED_HEROIN),
        "o8-2-1-2" => |ap| yes_if_contains(ap, DRUGS_INJECTED_HEROIN, Value::LAST_SIX),
        "o8-2-1-4" => |ap| yes_if_contains(ap, DRUGS_INJECTED_HEROIN, Value::MORE_THAN_SIX),
        "o8-2-2-1" => |ap| usage_frequency_score(ap, HOW_OFTEN_USED_LAST_SIX_MONTHS_METHADONE_NOT_PRESCRIBED),
        "o8-2-2-3" => |ap| yes_if_more_than_six(ap, DRUG_LAST_USED_METHADONE_NOT_PRESCRIBED),
        "o8-2-2-2" => |ap| yes_if_contains(ap, DRUGS_INJECTED_METHADONE_NOT_PRESCRIBED, Value::LAST_SIX),
        "o8-2-2-4" => |ap| yes_if_contains(ap, DRUGS_INJECTED_METHADONE_NOT_PRESCRIBED, Value::MORE_THAN_SIX),
        "o8-2-3-1" => |ap| usage_frequency_score(ap, HOW_OFTEN_USED_LAST_SIX_MONTHS_OTHER_OPIATES),
        "o8-2-3-3" => |ap| yes_if_more_than_six(ap, DRUG_LAST_USED_OTHER_OPIATES),
        "o8-2-3-2" => |ap| yes_if_contains(ap, DRUGS_INJECTED_OTHER_OPIATES, Value::LAST_SIX),
        "o8-2-3-4" => |ap| yes_if_contains(ap, DRUGS_INJECTED_OTHER_OPIATES, Value::MORE_THAN_SIX),
        "o8-2-4-1" => |ap| usage_frequency_score(ap, HOW_OFTEN_USED_LAST_SIX_MONTHS_CRACK),
        "o8-2-4-2" => |ap| yes_if_contains(ap, DRUGS_INJECTED_CRACK, Value::LAST_SIX),
        "o8-2-4-3" => |ap| yes_if_more_than_six(ap, DRUG_LAST_USED_CRACK),
        "o8-2-4-4" => |ap| yes_if_contains(ap, DRUGS_INJECTED_CRACK, Value::MORE_THAN_SIX),
        "o8-2-5-1" => |ap| usage_frequency_score(ap, HOW_OFTEN_USED_LAST_SIX_MONTHS_COCAINE),
        "o8-2-5-2" => |ap| yes_if_contains(ap, DRUGS_INJECTED_COCAINE, Value::LAST_SIX),
        "o8-2-5-3" => |ap| yes_if_more_than_six(ap, DRUG_LAST_USED_COCAINE),
        "o8-2-5-4" => |ap| yes_if_contains(ap, DRUGS_INJECTED_COCAINE, Value::MORE_THAN_SIX),
        "o8-2-6-1" => |ap| usage_frequency_score(ap, HOW_OFTEN_USED_LAST_SIX_MONTHS_MISUSED_PRESCRIBED_DRUGS),
        "o8-2-6-2" => |ap| yes_if_contains(ap, DRUGS_INJECTED_MISUSED_PRESCRIBED_DRUGS, Value::LAST_SIX),
        "o8-2-6-3" => |ap| yes_if_more_than_six(ap, DRUG_LAST_USED_MISUSED_PRESCRIBED_DRUGS),
        "o8-2-6-4" => |ap| yes_if_contains(ap, DRUGS_INJECTED_MISUSED_PRESCRIBED_DRUGS, Value::MORE_THAN_SIX),
        "o8-2-7-1" => |ap| usage_frequency_score(ap, HOW_OFTEN_USED_LAST_SIX_MONTHS_BENZODIAZEPINES),
        "o8-2-7-3" => |ap| yes_if_more_than_six(ap, DRUG_LAST_USED_BENZODIAZEPINES),
        "o8-2-7-2" => |ap| yes_if_contains(ap, DRUGS_INJECTED_BENZODIAZEPINES, Value::LAST_SIX),
        "o8-2-7-4" => |ap| yes_if_contains(ap, DRUGS_INJECTED_BENZODIAZEPINES, Value::MORE_THAN_SIX),
        "o8-2-8-1" => |ap| usage_frequency_score(ap, HOW_OFTEN_USED_LAST_SIX_MONTHS_AMPHETAMINES),
        "o8-2-8-2" => |ap| yes_if_contains(ap, DRUGS_INJECTED_AMPHETAMINES, Value::LAST_SIX),
        "o8-2-8-3" => |ap| yes_if_more_than_six(ap, DRUG_LAST_USED_AMPHETAMINES),
        "o8-2-8-4" => |ap| yes_if_contains(ap, DRUGS_INJECTED_AMPHETAMINES, Value::MORE_THAN_SIX),
        "o8-2-9-1" => |ap| usage_frequency_score(ap, HOW_OFTEN_USED_LAST_SIX_MONTHS_HALLUCINOGENICS),
        "o8-2-9-3" => |ap| yes_if_more_than_six(ap, DRUG_LAST_USED_HALLUCINOGENICS),
        "o8-2-10-1" => |ap| usage_frequency_score(ap, HOW_OFTEN_USED_LAST_SIX_MONTHS_ECSTASY),
        "o8-2-10-3" => |ap| yes_if_more_than_six(ap, DRUG_LAST_USED_ECSTASY),
        "o8-2-11-1" => |ap| usage_frequency_score(ap, HOW_OFTEN_USED_LAST_SIX_MONTHS_CANNABIS),
        "o8-2-11-3" => |ap| yes_if_more_than_six(ap, DRUG_LAST_USED_CANNABIS),
        "o8-2-12-1" => |ap| usage_frequency_score(ap, HOW_OFTEN_USED_LAST_SIX_MONTHS_SOLVENTS),
        "o8-2-12-3" => |ap| yes_if_more_than_six(ap, DRUG_LAST_USED_SOLVENTS),
        "o8-2-13-1" => |ap| usage_frequency_score(ap, HOW_OFTEN_USED_LAST_SIX_MONTHS_STEROIDS),
        "o8-2-13-2" => |ap| yes_if_contains(ap, DRUGS_INJECTED_STEROIDS, Value::LAST_SIX),
        "o8-2-13-3" => |ap| yes_if_more_than_six(ap, DRUG_LAST_USED_STEROIDS),
        "o8-2-13-4" => |ap| yes_if_contains(ap, DRUGS_INJECTED_STEROIDS, Value::MORE_THAN_SIX),
        "o8-2-15-1" => |ap| usage_frequency_score(ap, HOW_OFTEN_USED_LAST_SIX_MONTHS_SPICE),
        "o8-2-15-3" => |ap| yes_if_more_than_six(ap, DRUG_LAST_USED_SPICE),
        "o8-2-14-1" => |ap| usage_frequency_score(ap, HOW_OFTEN_USED_LAST_SIX_MONTHS_OTHER_DRUG),
        "o8-2-14-2" => |ap| yes_if_contains(ap, DRUGS_INJECTED_OTHER_DRUG, Value::LAST_SIX),
        "o8-2-14-3" => |ap| yes_if_more_than_six(ap, DRUG_LAST_USED_OTHER_DRUG),
        "o8-2-14-4" => |ap| yes_if_contains(ap, DRUGS_INJECTED_OTHER_DRUG, Value::MORE_THAN_SIX),
        "o8-2-14-t" => |ap| value_of(ap, OTHER_DRUG_NAME),
        "o8-4" => q4,
        "o8-5" => q5,
        "o8-6" => q6,
        "o8-8" => q8,
        "o8-97" => |ap| ANALYSIS.notes(ap),
        "o8-98" => |ap| ANALYSIS.risk_of_serious_harm(ap),
        "o8-99" => |ap| ANALYSIS.risk_of_reoffending(ap),
        "o8_SAN_STRENGTH" => |ap| ANALYSIS.strengths_or_protective_factors(ap),
    ])
}

fn usage_frequency_score(ap: &AnswersProvider, field: Field) -> Mapped {
    Ok(when_value!(ap, ap.answer(field)?.value()?, {
        DAILY => text("100"),
        WEEKLY => text("110"),
        MONTHLY => text("120"),
        OCCASIONALLY => text("130"),
        _ => NULL,
    }))
}

fn is_using(ap: &AnswersProvider, field: Field, frequencies: &[Value]) -> Result<bool> {
    let Some(usage) = ap.answer(field)?.value()? else {
        return Ok(false);
    };
    let options = frequencies
        .iter()
        .map(|frequency| ap.get(*frequency))
        .collect::<Result<Vec<_>>>()?;
    Ok(options.contains(&usage))
}

fn is_more_than_six(ap: &AnswersProvider, field: Field) -> Result<bool> {
    let last_used = ap.answer(field)?.value()?;
    ap.is(&last_used, Value::MORE_THAN_SIX)
}

fn is_last_six(ap: &AnswersProvider, field: Field) -> Result<bool> {
    let last_used = ap.answer(field)?.value()?;
    ap.is(&last_used, Value::LAST_SIX)
}

fn contains_more_than_six(ap: &AnswersProvider, field: Field) -> Result<bool> {
    let values = ap.answer(field)?.values()?.unwrap_or_default();
    ap.contains(&values, Value::MORE_THAN_SIX)
}

fn contains_last_six(ap: &AnswersProvider, field: Field) -> Result<bool> {
    let values = ap.answer(field)?.values()?.unwrap_or_default();
    ap.contains(&values, Value::LAST_SIX)
}

fn has_injected(ap: &AnswersProvider, drug: Value) -> Result<bool> {
    let values = ap.answer(Field::DRUGS_INJECTED)?.values()?.unwrap_or_default();
    ap.contains(&values, drug)
}

fn yes_if_more_than_six(ap: &AnswersProvider, field: Field) -> Mapped {
    Ok(if is_more_than_six(ap, field)? {
        text("YES")
    } else {
        NULL
    })
}

fn yes_if_contains(ap: &AnswersProvider, field: Field, value: Value) -> Mapped {
    Ok(match ap.answer(field)?.values()? {
        Some(values) if ap.contains(&values, value)? => text("YES"),
        _ => NULL,
    })
}

/// Short-circuiting `any`, in order, for fallible predicates.
fn any<T: Copy>(items: &[T], mut predicate: impl FnMut(T) -> Result<bool>) -> Result<bool> {
    for item in items {
        if predicate(*item)? {
            return Ok(true);
        }
    }
    Ok(false)
}

fn q1(ap: &AnswersProvider) -> Mapped {
    super::yes_no(ap, Field::DRUG_USE)
}

fn q4(ap: &AnswersProvider) -> Mapped {
    use Field::*;
    let frequencies = [Value::DAILY, Value::WEEKLY, Value::MONTHLY, Value::OCCASIONALLY];
    let drugs = [
        HOW_OFTEN_USED_LAST_SIX_MONTHS_HEROIN,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_METHADONE_NOT_PRESCRIBED,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_OTHER_OPIATES,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_CRACK,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_COCAINE,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_MISUSED_PRESCRIBED_DRUGS,
    ];

    Ok(if any(&drugs, |drug| is_using(ap, drug, &frequencies))? {
        text("2")
    } else {
        text("0")
    })
}

fn q5(ap: &AnswersProvider) -> Mapped {
    use Field::*;
    let daily_or_weekly = [Value::DAILY, Value::WEEKLY];
    let monthly_or_occasionally = [Value::MONTHLY, Value::OCCASIONALLY];
    let drugs = [
        HOW_OFTEN_USED_LAST_SIX_MONTHS_HEROIN,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_METHADONE_NOT_PRESCRIBED,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_OTHER_OPIATES,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_CRACK,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_COCAINE,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_MISUSED_PRESCRIBED_DRUGS,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_BENZODIAZEPINES,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_AMPHETAMINES,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_HALLUCINOGENICS,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_ECSTASY,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_CANNABIS,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_SOLVENTS,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_STEROIDS,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_SPICE,
        HOW_OFTEN_USED_LAST_SIX_MONTHS_OTHER_DRUG,
    ];
    let drugs_last_used = [
        DRUG_LAST_USED_AMPHETAMINES,
        DRUG_LAST_USED_BENZODIAZEPINES,
        DRUG_LAST_USED_CANNABIS,
        DRUG_LAST_USED_COCAINE,
        DRUG_LAST_USED_CRACK,
        DRUG_LAST_USED_ECSTASY,
        DRUG_LAST_USED_HALLUCINOGENICS,
        DRUG_LAST_USED_HEROIN,
        DRUG_LAST_USED_METHADONE_NOT_PRESCRIBED,
        DRUG_LAST_USED_MISUSED_PRESCRIBED_DRUGS,
        DRUG_LAST_USED_OTHER_OPIATES,
        DRUG_LAST_USED_SOLVENTS,
        DRUG_LAST_USED_STEROIDS,
        DRUG_LAST_USED_SPICE,
        DRUG_LAST_USED_OTHER_DRUG,
    ];

    Ok(if any(&drugs, |drug| is_using(ap, drug, &daily_or_weekly))? {
        text("2")
    } else if any(&drugs, |drug| is_using(ap, drug, &monthly_or_occasionally))? {
        text("0")
    } else if any(&drugs_last_used, |drug| is_last_six(ap, drug))? {
        text("M")
    } else if any(&drugs_last_used, |drug| is_more_than_six(ap, drug))? {
        text("0")
    } else {
        NULL
    })
}

fn q6(ap: &AnswersProvider) -> Mapped {
    use Field::*;
    let drugs_injected = [
        DRUGS_INJECTED_HEROIN,
        DRUGS_INJECTED_METHADONE_NOT_PRESCRIBED,
        DRUGS_INJECTED_OTHER_OPIATES,
        DRUGS_INJECTED_CRACK,
        DRUGS_INJECTED_COCAINE,
        DRUGS_INJECTED_MISUSED_PRESCRIBED_DRUGS,
        DRUGS_INJECTED_BENZODIAZEPINES,
        DRUGS_INJECTED_AMPHETAMINES,
        DRUGS_INJECTED_STEROIDS,
        DRUGS_INJECTED_OTHER_DRUG,
    ];
    let injectable_drugs = [
        Value::AMPHETAMINES,
        Value::BENZODIAZEPINES,
        Value::COCAINE,
        Value::CRACK,
        Value::HEROIN,
        Value::METHADONE_NOT_PRESCRIBED,
        Value::MISUSED_PRESCRIBED_DRUGS,
        Value::OTHER_OPIATES,
        Value::STEROIDS,
        Value::OTHER_DRUG,
    ];

    Ok(if any(&drugs_injected, |field| contains_last_six(ap, field))? {
        text("2")
    } else if any(&drugs_injected, |field| contains_more_than_six(ap, field))?
        || any(&injectable_drugs, |drug| has_injected(ap, drug))?
    {
        text("1")
    } else {
        text("0")
    })
}

fn q8(ap: &AnswersProvider) -> Mapped {
    Ok(
        when_value!(ap, ap.answer(Field::DRUGS_PRACTITIONER_ANALYSIS_MOTIVATED_TO_STOP)?.value()?, {
            FULL_MOTIVATION => text("0"),
            PARTIAL_MOTIVATION => text("1"),
            NO_MOTIVATION => text("2"),
            UNKNOWN => text("M"),
            _ => NULL,
        }),
    )
}
