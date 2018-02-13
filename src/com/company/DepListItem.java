package com.company;

public class DepListItem {
    String ZNI;
    String TBP;
    String Object;
    String Type;

    // Проверяем что объекты идентичны не учитывая номер ЗНИ
    boolean DepObjectsCheck(DepListItem checkItem)
    {
        if (checkItem !=null)
        {
            return (this.Type.equals(checkItem.Type) &&
                    this.TBP.equals(checkItem.TBP) && this.Object.equals(checkItem.Object));
        }
        return false;
    }
}
