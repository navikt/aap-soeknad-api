ALTER TABLE eksterneoppgavenotifikasjoner DROP CONSTRAINT eksternenotifikasjoner_oppgaveid_fkey;
ALTER TABLE eksternebeskjednotifikasjoner DROP CONSTRAINT eksternebeskjednotifikasjoner_beskjed_id_fkey;
ALTER TABLE eksternebeskjednotifikasjoner DROP CONSTRAINT fk_eventid_neskjed;