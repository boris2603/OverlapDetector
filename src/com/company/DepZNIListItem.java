package com.company;

import java.util.ArrayList;

public class DepZNIListItem {
    String ZNI;
    ArrayList<String> DependenceList;

    DepZNIListItem(String sZNI)
    {
        ZNI=sZNI;
        DependenceList=new ArrayList<String>();
    }

    // Проверим что ЗНИ есть в списках
    boolean CheckZNI(String TestZNI)
    {
        boolean Result=false;

        if (TestZNI.equals(ZNI))
        {
            Result=true;
        }
        else
        {
            for (String item : DependenceList)
            {
                if (item.equals(TestZNI))
                {
                    Result=true;
                    break;
                }
            }
        }
        return Result;
    }

}
