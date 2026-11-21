package com.redsaludandina.legacy.infrastructure.acl.job;

import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface JobStateRepository extends Repository<JobStateEntity, String> {

    Optional<JobStateEntity> findById(String name);

    JobStateEntity save(JobStateEntity state);
}
