<#-- $This file is distributed under the terms of the license in /doc/license.txt$ -->

<#-- Default individual search view -->

<#import "lib-vivo-properties.ftl" as p>

<a href="${individual.profileUrl}">${individual.name}</a>

<@p.displayTitle individual />

<p>${individual.snippet}</p>