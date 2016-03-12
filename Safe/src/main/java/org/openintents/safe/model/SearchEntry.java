/* 
 * Copyright (C) 2011-2012 OpenIntents.org
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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author Randy McEoin
 */
public class SearchEntry extends Object implements Parcelable {
    public long id = -1;
    public String name;
    public String category;
    public long categoryId = -1;

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public SearchEntry() {
        name = "";
        category = "";
    }

    public SearchEntry(String _name) {
        name = _name;
        category = "";
    }

    public SearchEntry(String _name, String _category) {
        name = _name;
        category = _category;
    }

    @Override
    public String toString() {
        return name + " " + category;
    }

    public int describeContents() {
        return 0;
    }

    /**
     * save object in parcel
     */
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeString(name);
        out.writeString(category);
        out.writeLong(categoryId);
    }

    public static final Parcelable.Creator<SearchEntry> CREATOR = new Parcelable.Creator<SearchEntry>() {
        public SearchEntry createFromParcel(Parcel in) {
            return new SearchEntry(in);
        }

        public SearchEntry[] newArray(int size) {
            return new SearchEntry[size];
        }
    };

    /**
     * recreate object from parcel
     */
    private SearchEntry(Parcel in) {
        id = in.readLong();
        name = in.readString();
        category = in.readString();
        categoryId = in.readLong();
    }
}
