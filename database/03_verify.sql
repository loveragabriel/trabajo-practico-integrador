-- row counts per table
SELECT 'zones' AS table_name, count(*) FROM zones
UNION ALL SELECT 'users', count(*) FROM users
UNION ALL SELECT 'walker_zones', count(*) FROM walker_zones
UNION ALL SELECT 'pets', count(*) FROM pets
UNION ALL SELECT 'walks', count(*) FROM walks
UNION ALL SELECT 'walk_pets', count(*) FROM walk_pets
UNION ALL SELECT 'reviews', count(*) FROM reviews;

-- duplicate zones (name + city), expect 0 rows
SELECT name, city, count(*)
FROM zones
GROUP BY name, city
HAVING count(*) > 1;

-- duplicate emails case-insensitive, expect 0 rows
SELECT LOWER(email), count(*)
FROM users
GROUP BY LOWER(email)
HAVING count(*) > 1;

-- owners with walker-only defaults set, expect 0 rows
SELECT id, email, role, default_price, default_capacity
FROM users
WHERE role = 'OWNER' AND (default_price IS NOT NULL OR default_capacity IS NOT NULL);

-- walks oversold: joined bookings exceeding capacity, expect 0 rows
SELECT w.id, w.capacity, count(wp.id) AS joined_count
FROM walks w
JOIN walk_pets wp ON wp.walk_id = w.id AND wp.status = 'JOINED'
GROUP BY w.id, w.capacity
HAVING count(wp.id) > w.capacity;

-- overlapping walks for the same walker, expect 0 rows
SELECT a.id AS walk_a, b.id AS walk_b, a.walker_id
FROM walks a
JOIN walks b ON a.walker_id = b.walker_id
    AND a.id < b.id
    AND a.status = 'PUBLISHED' AND b.status = 'PUBLISHED'
    AND a.start_time < b.end_time
    AND b.start_time < a.end_time;

-- walker rating_avg/review_count out of sync with reviews table, expect 0 rows
SELECT u.id, u.rating_avg, u.review_count,
       coalesce(r.avg_rating, 0) AS actual_avg,
       coalesce(r.review_count, 0) AS actual_count
FROM users u
LEFT JOIN (
    SELECT w.walker_id,
           round(avg(rv.rating), 1) AS avg_rating,
           count(rv.id) AS review_count
    FROM reviews rv
    JOIN walks w ON w.id = rv.walk_id
    GROUP BY w.walker_id
) r ON r.walker_id = u.id
WHERE u.role = 'WALKER'
  AND (u.rating_avg IS DISTINCT FROM coalesce(r.avg_rating, 0)
       OR u.review_count IS DISTINCT FROM coalesce(r.review_count, 0));
