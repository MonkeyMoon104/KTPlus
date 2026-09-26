package com.monkey.ktplus.api.service;

import com.monkey.ktplus.api.model.Effect;
import com.monkey.ktplus.api.model.EffectCategory;
import java.util.Collection;
import java.util.Optional;

/**
 * Catalog lookup for registered kill effects (builtin and external).
 *
 * <p>Ids and aliases are resolved case-insensitively in line with {@link Effect} identity.
 *
 * <p><b>Thread-safety:</b> call on the Bukkit main thread.
 *
 * @since 4.0.3
 */
public interface EffectService {
    /**
     * Finds an effect by primary id or registered alias.
     *
     * @param idOrAlias id or alias token
     * @return the effect if registered
     */
    Optional<Effect> find(String idOrAlias);

    /**
     * All registered effects in catalog order.
     *
     * @return collection of effects (possibly empty)
     */
    Collection<Effect> all();

    /**
     * Effects belonging to the given category.
     *
     * @param category category filter
     * @return matching effects
     */
    Collection<Effect> byCategory(EffectCategory category);

    /**
     * Primary ids of all registered effects.
     *
     * @return id collection
     */
    Collection<String> ids();

    /**
     * Whether an effect exists for the id or alias.
     *
     * @param idOrAlias id or alias
     * @return {@code true} if found
     */
    boolean exists(String idOrAlias);
}
