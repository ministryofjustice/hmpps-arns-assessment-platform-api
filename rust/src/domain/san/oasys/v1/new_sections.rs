use super::PractitionerAnalysis;
use crate::{
    domain::san::oasys::{
        FieldsToMap, Mapped, NULL,
        answers::AnswersProvider,
        codes::{Field, Value},
        fields, text,
    },
    error::Result,
};

const ANALYSIS: PractitionerAnalysis = PractitionerAnalysis("THINKING_BEHAVIOURS_ATTITUDES");

pub fn fields(_: &AnswersProvider) -> Result<FieldsToMap> {
    use Field::*;

    Ok(fields![
        "o1-30" => q30,
        "oTBA_SAN_LINKED_ROSH" => |ap| ANALYSIS.risk_of_serious_harm(ap),
        "oTBA_SAN_LINKED_REOFFEND" => |ap| ANALYSIS.risk_of_reoffending(ap),
        "oTBA_SAN_STRENGTH" => |ap| ANALYSIS.strengths_or_protective_factors(ap),
        "oAC_SAN_SECTION_COMP" => |ap| section_complete(ap, ACCOMMODATION_SECTION_COMPLETE),
        "oEE_SAN_SECTION_COMP" => |ap| section_complete(ap, EMPLOYMENT_EDUCATION_SECTION_COMPLETE),
        "oFI_SAN_SECTION_COMP" => |ap| section_complete(ap, FINANCE_SECTION_COMPLETE),
        "oSMD_SAN_SECTION_COMP" => |ap| section_complete(ap, DRUG_USE_SECTION_COMPLETE),
        "oSMA_SAN_SECTION_COMP" => |ap| section_complete(ap, ALCOHOL_USE_SECTION_COMPLETE),
        "oHW_SAN_SECTION_COMP" => |ap| section_complete(ap, HEALTH_WELLBEING_SECTION_COMPLETE),
        "oPRC_SAN_SECTION_COMP" => |ap| section_complete(ap, PERSONAL_RELATIONSHIPS_COMMUNITY_SECTION_COMPLETE),
        "oTBA_SAN_SECTION_COMP" => |ap| section_complete(ap, THINKING_BEHAVIOURS_ATTITUDES_SECTION_COMPLETE),
        "oOA_SAN_SECTION_COMP" => |ap| section_complete(ap, OFFENCE_ANALYSIS_SECTION_COMPLETE),
    ])
}

fn q30(ap: &AnswersProvider) -> Mapped {
    let elements = ap
        .answer(Field::OFFENCE_ANALYSIS_ELEMENTS)?
        .values()?
        .unwrap_or_default();
    let sexual_element = ap.contains(&elements, Value::SEXUAL_ELEMENT)?;
    let motivations = ap
        .answer(Field::OFFENCE_ANALYSIS_MOTIVATIONS)?
        .values()?
        .unwrap_or_default();
    let sexual_motivation = ap.contains(&motivations, Value::SEXUAL_MOTIVATION)?;

    Ok(if sexual_element || sexual_motivation {
        text("YES")
    } else {
        NULL
    })
}

fn section_complete(ap: &AnswersProvider, field: Field) -> Mapped {
    let complete = ap.answer(field)?.value()?;
    Ok(if ap.is(&complete, Value::YES)? {
        text("YES")
    } else {
        text("NO")
    })
}
