package com.company;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.nio.file.*;
import com.company.DepListItem;
import com.company.DepZNIListItem;

class ReleaseObject
{
    // Список всех объектов по ЗНИ
    @SuppressWarnings("WeakerAccess")
    final
    ArrayList<DepListItem> ReleaseFullItemsList = new ArrayList<>();

    // Спосок всех ЗНИ и отмеченных зависимостей
    @SuppressWarnings("WeakerAccess")
    final
    ArrayList<DepZNIListItem> ReleaseFullDepZNIList = new ArrayList<>();

    private final String FileObjectName = "ODObjectList.txt";
    private final String FileZNIName = "ODZNIList.txt";
    private final String FileNamePath;

    ReleaseObject(String ODFileNamePath)
    {
        FileNamePath=ODFileNamePath;
    }


    // Загрузить список ЗНИ по релизу
    @SuppressWarnings("ManualArrayToCollectionCopy")
    void LoadZNIList()
    {
        List<String> lines = new ArrayList<>();

        if (Files.exists(Paths.get(FileNamePath,FileZNIName))) {
            try {
                lines = Files.readAllLines(Paths.get(FileNamePath, FileZNIName), Charset.forName("windows-1251"));
            } catch (IOException e) {
                System.out.println("IO Error reading Objects File " + FileZNIName);
                System.out.println(e.getLocalizedMessage());
            }
        }

        ReleaseFullDepZNIList.clear();

        for(String line : lines) {
            String[] items = line.split(" ");
            if (items.length>0) {
                DepZNIListItem Item = new DepZNIListItem(items[0]);

                for (int idx = 1; idx < items.length; idx++) {
                    Item.DependenceList.add(items[idx]);
                }

                ReleaseFullDepZNIList.add(Item);
            }
        }

    }

    // Загрузить список объектов и зависимых ЗНИ по релизу
    void LoadItemList()
    {
        List<String> lines = new ArrayList<>();

        if (Files.exists(Paths.get(FileNamePath,FileObjectName))) {
            try {
                lines = Files.readAllLines(Paths.get(FileNamePath, FileObjectName), Charset.forName("windows-1251"));
            } catch (IOException e) {
                System.out.println("IO Error reading Objects File " + FileObjectName);
                System.out.println(e.getLocalizedMessage());
            }
        }

        ReleaseFullItemsList.clear();

        for(String line : lines) {
            String[] items = line.split(" ");
            DepListItem Item = new DepListItem();

            Item.ZNI=items[0];
            Item.TBP=items[2];
            Item.Type=items[1];
            Item.Object=items[3];

            ReleaseFullItemsList.add(Item);
        }

    }

    // Выгрузить список ЗНИ по релизу
    void SaveZNIList()
    {
        ArrayList<String> ObjectList= new ArrayList<>();


        for(DepZNIListItem item : ReleaseFullDepZNIList)
        {
            StringBuilder saveString = new StringBuilder(item.ZNI);
            for (String itmZNI : item.DependenceList)
            {
                if (!itmZNI.isEmpty()) {
                    saveString.append(" ").append(itmZNI);
                }
            }
            // saveString=item.ZNI+" "+saveString;
            ObjectList.add(saveString.toString());
        }

        try
        {
            if (Files.exists(Paths.get(FileNamePath,FileZNIName))){
                Files.delete(Paths.get(FileNamePath,FileZNIName));
            }
            Files.write(Paths.get(FileNamePath,FileZNIName), ObjectList, Charset.forName("windows-1251"), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
        }
        catch (IOException e)
        {
            System.out.println("IO Error writing Objects File "+FileZNIName);
            System.out.println(e.getLocalizedMessage());
            System.out.println(e.getMessage());
            //noinspection ThrowablePrintedToSystemOut
            System.out.println(e.fillInStackTrace());
        }

    }

    // Выгрузить список объектов и зависимых ЗНИ по релизу
    @SuppressWarnings("ThrowablePrintedToSystemOut")
    void SaveItemList()
    {
        ArrayList<String> ObjectList= new ArrayList<>();


        for(DepListItem item : ReleaseFullItemsList)
        {
            if (!item.ZNI.isEmpty()) {
                ObjectList.add(item.ZNI+" "+item.Type+" "+item.TBP+" "+item.Object);
            }
        }


       try
        {
            if (Files.exists(Paths.get(FileNamePath,FileObjectName))){
                Files.delete(Paths.get(FileNamePath,FileObjectName));
            }
            Files.write(Paths.get(FileNamePath,FileObjectName), ObjectList, Charset.forName("windows-1251"), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
        }
        catch (IOException e)
        {
            System.out.println("IO Error writing Objects File "+FileObjectName);
            System.out.println(e.getLocalizedMessage());
            System.out.println(e.getMessage());
            System.out.println(e.fillInStackTrace());
        }

    }

    // Список неразрешенных зависимостей по ЗНИ
    ArrayList<OverlapItem> OverlapDetector(InstallFile CheckInstallFile)
    // String ZNI, ArrayList<DepListItem> pZNIObjectList, ArrayList<String> DepZNIList)
    {

        // Список неразрешенных зависимостей по ЗНИ и объектам
        ArrayList<OverlapItem> ZNIIntersectionList= new ArrayList<>();

        OverlapItem  ZNIIntersectionItem = new OverlapItem(CheckInstallFile.sZNI);
        String CheckZNI="";

        // Получаем список всех зависимых объектов
        ArrayList<DepListItem> IntersectionObjects = OverlapCandidatDetector(CheckInstallFile.FullPCKItemsList);

        //Проверяем что объекты по ЗНИ учтены в списке работ
        for (DepListItem itemObject: IntersectionObjects)
        {
            // Проверяем что ЗНИ в объекте учтена в списке зависимых ЗНИ
            if (!Objects.equals(CheckInstallFile.sZNI, itemObject.ZNI)) {
                if (CheckInterseptZNI(itemObject.ZNI))
                {
                  if (!Objects.equals(CheckZNI, itemObject.ZNI))
                  {
                      ZNIIntersectionItem = new OverlapItem(itemObject.ZNI);
                  }
                  ZNIIntersectionItem.depListItems.add(itemObject);
                }
            }
            if (!ZNIIntersectionItem.depListItems.isEmpty()) {
                ZNIIntersectionList.add(ZNIIntersectionItem);
            }
        }

        // Проверим что ЗНИ от которых есть зависимость передавалось на стенд
        if (!CheckInstallFile.DepZNIList.isEmpty())
        {
            for (String TestZNI : CheckInstallFile.DepZNIList)
            {
                if (!AlreadyInstallZNI(TestZNI))
                {
                    ZNIIntersectionItem = new OverlapItem(TestZNI);
                    ZNIIntersectionList.add(ZNIIntersectionItem);
                }
            }
        }

       // ArrayList<String> DetectedDependZNI=OverlapZNIDetector(DetectedObjectList);

        return ZNIIntersectionList;
    }

    // Проверим что ЗНИ устанавливалось на стенд
    private boolean AlreadyInstallZNI(String CheckZNI)
    {
        boolean retval=false;
        for (DepZNIListItem itemZNI : ReleaseFullDepZNIList) {

            if (itemZNI.ZNI.equals(CheckZNI))
            {
                retval=true;
                break;
            }
        }
        return retval;
    }

    // Проверим что по ЗНИ не учтены звиссимости
    private boolean CheckInterseptZNI(String CheckZNI){

        int depZNIPresentCount=0;
        for (DepZNIListItem depZNI: ReleaseFullDepZNIList )
            {
                if (depZNI.CheckZNI(CheckZNI)) {
                    depZNIPresentCount++;
                }
            }
            return (depZNIPresentCount==1);
    }


    // Заменить список ЗНИ в релизе
    void ChangeReleaseZNIList(String AddZNI, ArrayList<String> ZNIList)
    {
        DepZNIListItem RemoveItem=new DepZNIListItem("");
        for(DepZNIListItem item: ReleaseFullDepZNIList)
        {
            if (item.ZNI.equals(AddZNI))
            {
                RemoveItem=item;
                break;
            }
        }
        if (!RemoveItem.ZNI.isEmpty()) {
            ReleaseFullDepZNIList.remove(RemoveItem);
        }

        DepZNIListItem AddItem =new DepZNIListItem(AddZNI);
        AddItem.DependenceList=ZNIList;
        if (!AddItem.ZNI.isEmpty()) {
            ReleaseFullDepZNIList.add(AddItem);
        }
    }

    // Заменить список объектов по ЗНИ в обзщем релизе
    void ChangeReleaseItemList(String ZNI, ArrayList<DepListItem> pZNIObjectList)
    {
        ArrayList<DepListItem> RemoveFullItemsList = new ArrayList<>();
        //Удалить все записи по ЗНИ
        for (DepListItem Item : ReleaseFullItemsList) {
            if (Item.ZNI.equals(ZNI))
            {
                RemoveFullItemsList.add(Item);
            }
        }
        // Добавляем свежие объекты
        ReleaseFullItemsList.removeAll(RemoveFullItemsList);
        ReleaseFullItemsList.addAll(pZNIObjectList);
    }

    // Определить список объектов по которым есть пересечения
    private ArrayList<DepListItem> OverlapCandidatDetector(ArrayList<DepListItem> pZNIObjectList)
    {
        ArrayList<DepListItem> OverlapItems = new ArrayList<>();
        for(DepListItem Item:pZNIObjectList)
        {
            ArrayList<DepListItem> itemCandidate = FindObjects(Item);
            OverlapItems.addAll(itemCandidate);
        }
        return OverlapItems;
    }

    // Определить есть-ли объект не по этому ЗНИ в релизе ранее
    private ArrayList<DepListItem> FindObjects(DepListItem checkItem)
    {
        ArrayList<DepListItem> OverlapItems = new ArrayList<>();

        for(DepListItem Item:ReleaseFullItemsList)
        {
          if (!Item.ZNI.equals(checkItem.ZNI) && Item.DepObjectsCheck(checkItem)) {
              OverlapItems.add(Item);
          }
        }
        return OverlapItems;
    }

}
