/*
 * Author: Balch
 * Created: 9/4/14 12:26 AM
 *
 * This file is part of MockTrade.
 *
 * MockTrade is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MockTrade is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MockTrade.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2014
 */

package com.balch.mocktrade.portfolio;


import android.content.ContentValues;
import android.database.Cursor;

import com.balch.android.app.framework.sql.SqlMapper;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.model.ModelProvider;
import com.balch.mocktrade.model.SqliteModel;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class SnapshotTotalsSqliteModel extends SqliteModel
        implements SqlMapper<PerformanceItem>, Serializable {
    public static final String TAG = SnapshotTotalsSqliteModel.class.getSimpleName();

    public static final String TABLE_NAME = "snapshot_totals";

    public static final String COLUMN_ACCOUNT_ID = "account_id";
    public static final String COLUMN_SNAPSHOT_TIME = "snapshot_time";
    public static final String COLUMN_TOTAL_VALUE = "total_value";
    public static final String COLUMN_COST_BASIS = "cost_basis";
    public static final String COLUMN_TODAY_CHANGE = "today_change";

    public SnapshotTotalsSqliteModel(ModelProvider modelProvider) {
        super(modelProvider);
    }

    @Override
    public String getTableName() {
        return SnapshotTotalsSqliteModel.TABLE_NAME;
    }

    @Override
    public ContentValues getContentValues(PerformanceItem performanceItem) {
        ContentValues values = new ContentValues();

        values.put(COLUMN_ACCOUNT_ID, performanceItem.getAccountId());
        values.put(COLUMN_SNAPSHOT_TIME, performanceItem.mTimestamp.getTime());
        values.put(COLUMN_COST_BASIS, performanceItem.getCostBasis().getMicroCents());
        values.put(COLUMN_TOTAL_VALUE, performanceItem.getValue().getMicroCents());
        values.put(COLUMN_TODAY_CHANGE, performanceItem.getTodayChange().getMicroCents());

        return values;
    }

    @Override
    public void populate(PerformanceItem performanceItem, Cursor cursor, Map<String, Integer> columnMap) {
        performanceItem.setAccountId(cursor.getLong(columnMap.get(COLUMN_ACCOUNT_ID)));
        performanceItem.setTimestamp(new Date(cursor.getLong(columnMap.get(COLUMN_SNAPSHOT_TIME))));
        performanceItem.setCostBasis(new Money(cursor.getLong(columnMap.get(COLUMN_COST_BASIS))));
        performanceItem.setValue(new Money(cursor.getLong(columnMap.get(COLUMN_TOTAL_VALUE))));
        performanceItem.setTodayChange(new Money(cursor.getLong(columnMap.get(COLUMN_TODAY_CHANGE))));
    }


    public Date getLastSyncTime() {
        Date date = null;
        Cursor cursor = null;
        try {
            cursor = getSqlConnection().getReadableDatabase().
                    rawQuery("select max("+COLUMN_SNAPSHOT_TIME+") from "+TABLE_NAME, new String[0]);
            if (cursor.moveToNext()) {
                date = new Date(cursor.getLong(0));
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return date;
    }


}
