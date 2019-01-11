package com.graphhopper.storage;

import com.graphhopper.storage.Lock;
import java.io.File;

public interface LockFactory {
   void setLockDir(File var1);

   Lock create(String var1, boolean var2);

   void forceRemove(String var1, boolean var2);
}
