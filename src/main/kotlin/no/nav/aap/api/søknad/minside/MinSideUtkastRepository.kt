package no.nav.aap.api.søknad.minside

import java.util.*
import javax.persistence.Entity
import javax.persistence.EnumType.STRING
import javax.persistence.Enumerated
import javax.persistence.Table
import no.nav.aap.api.felles.SkjemaType
import no.nav.aap.api.felles.SkjemaType.STANDARD
import no.nav.aap.api.søknad.minside.MinSideRepository.MinSideBaseEntity
import no.nav.aap.api.søknad.minside.MinSideUtkastRepository.Utkast
import no.nav.aap.util.StringExtensions.partialMask

interface MinSideUtkastRepository : MinSideRepository<Utkast> {
    fun existsByFnrAndSkjematype(fnr: String, skjemaType: SkjemaType): Boolean
    fun findByFnrAndSkjematype(fnr: String, skjemaType: SkjemaType): Utkast?


    @Entity(name = "utkast")
    @Table(name = "minsideutkast")
    class Utkast(fnr: String, eventid: UUID,
                 @Enumerated(STRING) var type: UtkastType,
                 @Enumerated(STRING) var skjematype: SkjemaType = STANDARD,
                 done: Boolean = false) : MinSideBaseEntity(fnr,eventid,done,false) {
        override fun toString() = "${javaClass.simpleName} [fnr=${fnr.partialMask()}, skjemaType = $skjematype, type = $type, created=$created, eventid=$eventid, updated=$updated, done=$done,id=$id]"
    }
}