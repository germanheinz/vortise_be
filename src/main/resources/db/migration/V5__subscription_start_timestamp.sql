-- Stripe start_date; historical records remain unknown until a later synchronization.
ALTER TABLE suscripciones ADD COLUMN inicio TIMESTAMP WITH TIME ZONE;
