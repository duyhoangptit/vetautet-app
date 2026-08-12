
package com.vetautet.app.domain.ticketing.repository;

import com.vetautet.app.domain.ticketing.model.Train;
import com.vetautet.app.domain.ticketing.model.TrainStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainRepository {

    Train save(Train train);

    Optional<Train> findById(UUID trainId);

    Optional<Train> findByCode(String trainCode);

    List<Train> findByStatus(TrainStatus status);
}