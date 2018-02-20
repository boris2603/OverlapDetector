package com.company;

import com.company.DepListItem;

import java.util.ArrayList;

// Зависимости по конкретному ЗНИ
class OverlapItem {
    final String mainZNI;
    String Developer;
    final ArrayList<DepListItem> depListItems;
    final ArrayList<String> EmailList;

    OverlapItem(String ZNI)
    {
        mainZNI=ZNI;
        Developer="";
        depListItems= new ArrayList<>();
        EmailList= new ArrayList<>();
    }
}
