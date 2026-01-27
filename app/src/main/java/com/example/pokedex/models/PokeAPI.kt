package com.example.pokedex.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class PokeList(
    val count: Long,
    val next: String?,
    val previous: String?,
    val results: List<ApiResult>,
)

@Serializable
data class ApiResult(
    val name: String,
    val url: String,
)

@Serializable
data class PokemonInfo(
    val abilities: List<Ability>,
    @SerialName("base_experience")
    val baseExperience: Long,
    val forms: List<Form>,
    @SerialName("game_indices")
    val gameIndices: List<Index>,
    val height: Long,
    @SerialName("held_items")
    val heldItems: List<HeldItem>,
    val id: Long,
    @SerialName("is_default")
    val isDefault: Boolean,
    @SerialName("location_area_encounters")
    val locationAreaEncounters: String,
    val moves: List<Mfe>,
    val name: String,
    val order: Long,
    val species: Species,
    val sprites: Sprites,
    val stats: List<Stat>,
    val types: List<Type>,
    val weight: Long,
)

@Serializable
data class Ability(
    val ability: AbilityInfo,
    @SerialName("is_hidden")
    val isHidden: Boolean,
    val slot: Long,
)

@Serializable
data class AbilityInfo(
    val name: String,
    val url: String,
)

@Serializable
data class Form(
    val name: String,
    val url: String,
)

@Serializable
data class Index(
    @SerialName("game_index")
    val gameIndex: Long,
    val version: Version,
)

@Serializable
data class Version(
    val name: String,
    val url: String,
)

@Serializable
data class HeldItem(
    val item: Item,
    @SerialName("version_details")
    val versionDetails: List<VersionDetail>,
)

@Serializable
data class Item(
    val name: String,
    val url: String,
)

@Serializable
data class VersionDetail(
    val rarity: Long,
    val version: Version2,
)

@Serializable
data class Version2(
    val name: String,
    val url: String,
)

@Serializable
data class Mfe(
    val move: Move,
    @SerialName("version_group_details")
    val versionGroupDetails: List<VersionGroupDetail>,
)

@Serializable
data class Move(
    val name: String,
    val url: String,
)

@Serializable
data class VersionGroupDetail(
    @SerialName("level_learned_at")
    val levelLearnedAt: Long,
    @SerialName("move_learn_method")
    val moveLearnMethod: MoveLearnMethod,
    val order: Int?,
    @SerialName("version_group")
    val versionGroup: VersionGroup,
)

@Serializable
data class MoveLearnMethod(
    val name: String,
    val url: String,
)

@Serializable
data class VersionGroup(
    val name: String,
    val url: String,
)


@Serializable
data class Species(
    val name: String,
    val url: String,
)

@Serializable
data class Sprites(
    @SerialName("back_default")
    val backDefault: String,
    @SerialName("back_female")
    val backFemale: String?,
    @SerialName("back_shiny")
    val backShiny: String,
    @SerialName("back_shiny_female")
    val backShinyFemale: String?,
    @SerialName("front_default")
    val frontDefault: String,
    @SerialName("front_female")
    val frontFemale: String?,
    @SerialName("front_shiny")
    val frontShiny: String,
    @SerialName("front_shiny_female")
    val frontShinyFemale: String?,
)

@Serializable
data class Stat(
    @SerialName("base_stat")
    val baseStat: Long,
    val effort: Long,
    val stat: StatNane,
)

@Serializable
data class StatNane(
    val name: String,
    val url: String,
)

@Serializable
data class Type(
    val slot: Long,
    val type: TypeName,
)

@Serializable
data class TypeName(
    val name: String,
    val url: String,
)


class PokeAPIRepository {
    lateinit var pokeAPI: PokeAPI
    init {
        val BASE_URL =
            "https://pokeapi.co/api/v2/"


            val logging = HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)
            val client: OkHttpClient = OkHttpClient.Builder().addInterceptor(logging).build()


         val json = Json { ignoreUnknownKeys = true }
         val retrofit = Retrofit.Builder()
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .baseUrl(BASE_URL).client(client).build()
         pokeAPI = retrofit.create<PokeAPI>()
    }

    suspend fun list(offset: Int): PokeList {
        return pokeAPI.list(offset, 100)
    }

    suspend fun getPokemon(name: String): PokemonInfo {
        return pokeAPI.getPokemon(name)
    }
}

interface PokeAPI {
    @GET("pokemon")
    suspend fun list(@Query("offset") offset: Int, @Query("limit") limit: Int): PokeList

    @GET("pokemon/{name}")
    suspend fun getPokemon(@Path("name") name: String): PokemonInfo
}
