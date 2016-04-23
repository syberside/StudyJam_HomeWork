package ru.xrm.syber.currencyexchanger;

import java.util.Comparator;

public class CompareByCode implements Comparator<CurrencyInfo> {
    @Override
    public int compare(CurrencyInfo o1, CurrencyInfo o2) {
        return o1.VchCode.compareTo(o2.VchCode);
    }
}
