package com.company;

import java.util.ArrayList;

class DepZNIListItem {
    final String ZNI;
    ArrayList<String> DependenceList;
    String Developer;
    ArrayList<String> eMilList;

    DepZNIListItem(String sZNI,String sDeveloper)
    {
        ZNI=sZNI;
        DependenceList= new ArrayList<>();
        eMilList=new ArrayList<>();
        Developer=sDeveloper;
    }

    // Проверим что ЗНИ есть в списках зависимых
    boolean CheckZNI(String TestZNI)
    {
        boolean retval=false;

        if (!DependenceList.isEmpty()) {
            for (String item : DependenceList)
            {
                if (item.equals(TestZNI))
                {
                    retval=true;
                    break;
                }
            }
        }
        return retval;
    }
}
