package com.company;

import java.util.ArrayList;

class DepZNIListItem {
    String ZNI;
    String Distributive;
    ArrayList<String> DependenceList;
    ArrayList<String> AlsoReleasedList;
    String Developer;
    ArrayList<String> eMilList;

    DepZNIListItem(String sZNI,String sDeveloper, String sDistributive)
    {
        ZNI=sZNI;
        DependenceList= new ArrayList<String>();
        AlsoReleasedList= new ArrayList<String>();
        eMilList=new ArrayList<String>();
        Developer=sDeveloper;
        Distributive=sDistributive;
    }

    // Проверим что ЗНИ есть в списках зависимых
    boolean CheckZNI(String TestZNI)
    {
        boolean retval=false;

        // Проверим что ЗНИ есть в списке зависимых ЗНИ
        if (!DependenceList.isEmpty())
            for (int i = 0; i < DependenceList.size(); i++) {
            String item = DependenceList.get(i);
            if (item.equals(TestZNI)) {
                retval = true;
                break;
            }
        }

        // Проверим что ЗНИ есть в списке реализованных в одном дистрибутиве
        if (!retval  && !AlsoReleasedList.isEmpty())
        {
            for (String item : AlsoReleasedList)
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
