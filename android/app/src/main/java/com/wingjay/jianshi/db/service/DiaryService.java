package com.wingjay.jianshi.db.service;


import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.wingjay.jianshi.db.model.Diary;
import com.wingjay.jianshi.db.model.Diary_Table;
import com.wingjay.jianshi.di.ForApplication;
import com.wingjay.jianshi.sync.Change;
import com.wingjay.jianshi.sync.Operation;
import com.wingjay.jianshi.sync.SyncService;
import com.wingjay.jianshi.util.DateUtil;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

public class DiaryService {

  private Context context;

  @Inject
  private Gson gson;

  @Inject
  DiaryService(@ForApplication Context context) {
    this.context = context;
  }

  public Observable<Void> saveDiary(final Diary diary) {
    return Observable.defer(new Func0<Observable<Void>>() {
      @Override
      public Observable<Void> call() {
        JsonObject jsonObject = new JsonObject();
        diary.setTime(DateUtil.getCurrentTimeStamp());
        if (diary.getTime_removed() > 0) {
          jsonObject.add(Operation.DELETE.getAction(), gson.toJsonTree(diary));
        } else if (diary.getTime_modified() >= diary.getTime_created()) {
          jsonObject.add(Operation.UPDATE.getAction(), gson.toJsonTree(diary));
        } else {
          jsonObject.add(Operation.CREATE.getAction(), gson.toJsonTree(diary));
        }
        Change.handleChangeByDBKey(Change.DBKey.DIARY, jsonObject);
        diary.save();
        SyncService.syncImmediately(context);
        return Observable.just(null);
      }
    }).subscribeOn(Schedulers.io());
  }

  public Observable<List<Diary>> getDiaryList() {
    return Observable.defer(new Func0<Observable<List<Diary>>>() {
      @Override
      public Observable<List<Diary>> call() {
        return Observable.just(fetchDiaryListFromDB());
      }
    }).subscribeOn(Schedulers.io());
  }

  private List<Diary> fetchDiaryListFromDB() {
    return SQLite.select()
        .from(Diary.class)
        .where(Diary_Table.time_removed.eq(0))
        .queryList();
  }

  public Observable<Diary> getDiaryByUuid(final String uuid) {
    return Observable.defer(new Func0<Observable<Diary>>() {
      @Override
      public Observable<Diary> call() {
        Diary diary = SQLite
            .select()
            .from(Diary.class)
            .where(Diary_Table.uuid.is(uuid))
            .querySingle();
        return Observable.just(diary);
      }
    }).subscribeOn(Schedulers.io());
  }
}
