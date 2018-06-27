package com.company;

import com.company.DepListItem;

import java.util.ArrayList;

// Зависимости по конкретному ЗНИ
class OverlapItem {
    final String mainZNI;
    String Developer;
    final ArrayList<String> EmailList;
    final ArrayList<DepListItem> depListItems;

    OverlapItem(String ZNI)
    {
        mainZNI = ZNI;

        Developer = "";
        depListItems = new ArrayList<>();
        EmailList = new ArrayList<>();
    }
}
