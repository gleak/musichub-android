package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

/**
 * Full playlist payload returned by `GET /api/playlists/{id}`.
 * The [songs] list is ordered — index 0 plays first.
 */
@Serializable
data class PlaylistDetailDto(
    val id: Long,
    val name: String,
    val createdAt: String,
    val updatedAt: String,
    val songs: List<PlaylistSongEntryDto>,
    val kind: String = "USER",
    val lastRefreshedAt: String? = null,
    val autoSync: Boolean = false,
    /**
     * Sharing fields — see [PlaylistDto] for forward-compat rationale.
     * The detail screen uses these to: (a) show "Shared by X" sub-header,
     * (b) hide the auto-sync card for non-owners, (c) flip the destructive
     * action label between "Delete playlist" (owner) and "Remove from
     * library" (member).
     */
    val ownerId: Long? = null,
    val ownerName: String? = null,
    val isOwner: Boolean = true,
    val memberCount: Int = 0,
    /**
     * True quando a comporre questa playlist e' stato il DJ.
     *
     * Sopravvive alla promozione, che porta [kind] da `DJ_SET` a `USER`: da
     * quel momento [kind] non distingue piu' una proposta accettata da una
     * playlist scritta a mano, e senza questo campo l'utente si ritroverebbe
     * in libreria playlist che non ricorda di aver creato.
     *
     * Default `false` per compatibilita' con backend precedenti a questa
     * colonna: nel dubbio, non marchiare — un falso "proposta del DJ" su una
     * playlist scritta dall'utente e' peggio di un marchio mancante.
     */
    val createdByDj: Boolean = false,
) {
    val isAuto: Boolean get() = kind != "USER"

    /** Lo slot del DJ non ancora promosso: si riscrive da solo a ogni ciclo. */
    val isDjSet: Boolean get() = kind == "DJ_SET"

    /**
     * Va mostrata come proposta dell'AI: o e' ancora uno slot, o e' una
     * proposta che l'utente ha promosso e tenuto.
     */
    val fromDj: Boolean get() = isDjSet || createdByDj
    val isShared: Boolean get() = !isOwner || memberCount > 0
}
