package com.gmnl.orientation.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ProgressRepository extends JpaRepository<Progress, ProgressId> {
  List<Progress> findByUserId(Long userId);
  long countByDocId(Long docId);

  /**
   * 原子 upsert:并发首写不再撞 (user_id, doc_id) 主键。
   * 业务语义在 SQL 内等价实现:
   * <ul>
   *   <li>progress_pct 只增不减(GREATEST);</li>
   *   <li>状态只升不降,COMPLETED 不被 READING/NOT_STARTED 回退;</li>
   *   <li>completed_at 只在首次完成时写入(已有值优先);</li>
   *   <li>last_read_at 仅在本次上报携带新值时更新,否则保留原值。</li>
   * </ul>
   */
  @Modifying
  @Query(value = """
      INSERT INTO progress (user_id, doc_id, status, progress_pct, last_read_at, completed_at)
      VALUES (:userId, :docId, :status, :pct, :lastReadAt, :completedAt)
      ON CONFLICT (user_id, doc_id) DO UPDATE SET
        progress_pct = GREATEST(progress.progress_pct, EXCLUDED.progress_pct),
        status = CASE
          WHEN progress.status = 'COMPLETED' OR EXCLUDED.status = 'COMPLETED' THEN 'COMPLETED'
          WHEN progress.status = 'READING' OR EXCLUDED.status = 'READING' THEN 'READING'
          ELSE 'NOT_STARTED'
        END,
        last_read_at = COALESCE(EXCLUDED.last_read_at, progress.last_read_at),
        completed_at = COALESCE(progress.completed_at, EXCLUDED.completed_at)
      """, nativeQuery = true)
  int upsertAtomic(@Param("userId") Long userId, @Param("docId") Long docId,
                   @Param("status") String status, @Param("pct") int pct,
                   @Param("lastReadAt") Instant lastReadAt, @Param("completedAt") Instant completedAt);
}
