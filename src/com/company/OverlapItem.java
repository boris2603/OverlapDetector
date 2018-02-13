package com.company;

import com.company.DepListItem;

import java.util.ArrayList;

// Зависимости по конкретному ЗНИ
class OverlapItem {
    final String mainZNI;
    final ArrayList<DepListItem> depListItems;

    OverlapItem(String ZNI)
    {
        mainZNI=ZNI;
        depListItems= new ArrayList<>();
    }
}
