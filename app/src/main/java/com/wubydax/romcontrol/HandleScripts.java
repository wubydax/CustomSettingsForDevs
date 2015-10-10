package com.wubydax.romcontrol;

import android.content.Context;
import android.content.res.AssetManager;
import android.widget.Toast;

import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

/*      Created by Roberto Mariani and Anna Berkovitch, 19/06/15
        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
public class HandleScripts {
    Context c;
    AssetManager am;
    //Our folder containing scripts will have the same name in both assets and files in /data/data/<package name>/files
    //Make sure when you create a script it ends with .sh and when you create a key in preferences it's called #script#filename without the .sh
    String scriptFolderName = "scripts";
    String[] scriptsInAssets, scriptsInFiles;
    String scriptFilesDirPath;

    //Main constructor. we only need context to access all necessary classes and methods
    public HandleScripts(Context context){
        this.c = context;
    }
    public boolean copyAssetFolder() {

        try {
            am = c.getAssets();
            scriptsInAssets = am.list(scriptFolderName);
            scriptFilesDirPath = c.getFilesDir().getPath() + File.separator + scriptFolderName;
            File scriptsFilesDir = new File(scriptFilesDirPath);
            //Checking if the "scripts" directory exists in files
            if(!scriptsFilesDir.exists()) {
                new File(scriptFilesDirPath).mkdirs();
            }
            boolean res = true;
            for (String file : scriptsInAssets)
            //If the file name contains  a dot, it's most probably a single file and not dir. So treating it as copying file
                if (file.contains("."))
                    res &= copyAsset(scriptFolderName + File.separator + file, scriptFilesDirPath + File.separator + file);
                else
            //Otherwise treating as copying dir
                    res &= copyAssetFolder();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean copyAsset(String from, String to) {
        boolean isCopied = false;
        InputStream in = null;
        OutputStream out = null;
        ArrayList<File> scriptsFiles = new ArrayList<>();
        //Creating list of File objects inside assets
        for(int i=0; i<scriptsInAssets.length; i++){
            File f = new File(scriptFilesDirPath + File.separator + scriptsInAssets[i]);
            scriptsFiles.add(f);
        }
        for(int j=0; j<scriptsFiles.size(); j++){
            //If the file doesn't exist in files dir, we copy it
            if(!scriptsFiles.get(j).exists()){
                try {
                    in = am.open(from);
                    new File(to).createNewFile();
                    out = new FileOutputStream(to);
                    copyFile(in, out);
                    in.close();
                    in = null;
                    out.flush();
                    out.close();
                    out = null;
                    isCopied = true;
                } catch(Exception e) {
                    e.printStackTrace();
                    isCopied = false;
                }
            }
        }
        File parent = new File(scriptFilesDirPath);
       // scriptsInFiles = parent.list();
        //If the file was just copied, we make it executable
        if(isCopied){

               try {
                   Command c = new Command(0,"chmod -R 755 " + scriptFilesDirPath);
                   RootTools.getShell(false).add(c);

               } catch (IOException e) {
                   e.printStackTrace();
               } catch (RootDeniedException e) {
                   e.printStackTrace();
               } catch (TimeoutException e) {
                   e.printStackTrace();
               }
           }

        return isCopied;
    }
    //Actual copying of the file
    public void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

}
