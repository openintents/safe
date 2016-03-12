/* $Id$
 * 
 * Copyright 2008 Randy McEoin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openintents.safe.model;

/**
 * @author Randy McEoin
 */
public class CategoryEntry extends Object {
    public long id = -1;
    public String name;
    public boolean nameNeedsDecrypt;
    public String plainName;
    public boolean plainNameNeedsEncrypt = true;
    private int count = 0;

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    public CategoryEntry() {
        name = "";
    }

    public CategoryEntry(String _name) {
        name = _name;
    }

    public CategoryEntry(String _name, int _count) {
        name = _name;
        count = _count;
    }

    @Override
    public String toString() {
        return name + " " + count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
