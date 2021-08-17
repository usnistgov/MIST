package gov.nist.isg.mist.lib.export;

import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.unit.Unit;

public enum MicroscopyUnits {
    KILOMETER(UNITS.KILOMETER),
    HECTOMETER(UNITS.HECTOMETER),
    DECAMETER(UNITS.DECAMETER),
    METER(UNITS.METER),
    DECIMETER(UNITS.DECIMETER),
    CENTIMETER(UNITS.CENTIMETER),
    MILLIMETER(UNITS.MILLIMETER),
    MICROMETER(UNITS.MICROMETER),
    NANOMETER(UNITS.NANOMETER),
    PICOMETER(UNITS.PICOMETER),
    ;
    MicroscopyUnits(Unit<Length> unit) {
        this.unit = unit;
    }

    private Unit<Length> unit;

    public Unit<Length> getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return this.name();
    }
}
