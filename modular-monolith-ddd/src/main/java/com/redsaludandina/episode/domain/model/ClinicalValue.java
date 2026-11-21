package com.redsaludandina.episode.domain.model;

import com.redsaludandina.shared.domain.CenterStamp;
import com.redsaludandina.shared.domain.Require;

/** Valor de un campo clínico con el sello del centro que lo escribió. */
public record ClinicalValue(String text, CenterStamp stamp) {

    public ClinicalValue {
        text = Require.text(text, "texto clínico");
        Require.notNull(stamp, "stamp");
    }
}
