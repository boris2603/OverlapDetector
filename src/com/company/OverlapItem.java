package com.company;

import com.company.DepListItem;

import java.util.ArrayList;

// Зависимости по конкретному ЗНИ
public class OverlapItem {
    String mainZNI;
    ArrayList<DepListItem> depListItems;

    OverlapItem(String ZNI)
    {
        mainZNI=ZNI;
        depListItems=new ArrayList<DepListItem>();
    }
}
