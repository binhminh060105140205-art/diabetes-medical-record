WITH ranked_orders AS (
    SELECT lab_order_id,
           ROW_NUMBER() OVER (
               PARTITION BY encounter_id, UPPER(test_code)
               ORDER BY CASE status
                            WHEN 'REVIEWED' THEN 1
                            WHEN 'RESULTED' THEN 2
                            WHEN 'COLLECTED' THEN 3
                            WHEN 'ORDERED' THEN 4
                            ELSE 5
                        END,
                        COALESCE(resulted_at, ordered_at) DESC,
                        lab_order_id DESC
           ) AS duplicate_rank
    FROM lab_orders
    WHERE status <> 'CANCELLED'
)
UPDATE lab_orders current_order
SET status = 'CANCELLED'
FROM ranked_orders ranked
WHERE current_order.lab_order_id = ranked.lab_order_id
  AND ranked.duplicate_rank > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_lab_active_test_per_encounter
    ON lab_orders(encounter_id, UPPER(test_code))
    WHERE status <> 'CANCELLED';
