package com.example.timemaster.ui.checkin;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.timemaster.data.repository.TimeRepository;
import com.example.timemaster.data.repository.TimeRepositoryImpl;

public class TimeViewModelFactory implements ViewModelProvider.Factory {

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(TimeViewModel.class)) {
            TimeRepository repository = new TimeRepositoryImpl();
            return (T) new TimeViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
