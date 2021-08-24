import React, { useContext, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import moment from 'moment'
import I18n from 'i18n-js'
import ReactTooltip from 'react-tooltip'
import { getIdpRolesWithUsers } from '../api'
import Breadcrumbs from '../components/breadcrumbs'
import ContactPerson from '../components/contact_person'
import ServicesForIdp from '../components/services_for_idp'
import { CurrentUserContext } from '../App'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faQuestionCircle } from '@fortawesome/free-regular-svg-icons'
import groupBy from 'lodash.groupby'

export default function MyIdp() {
  const { currentUser } = useContext(CurrentUserContext)
  const [roles, setRoles] = useState({})
  const breadcrumbs = [
    { link: '/apps/connected', text: 'Home' },
    { link: `/my-idp`, text: I18n.t('navigation.my_idp') },
  ]

  const currentIdp = currentUser.getCurrentIdp()
  const isDashboardAdmin = currentUser.dashboardAdmin

  async function fetchIdp() {
    const data = await getIdpRolesWithUsers()
    setRoles(data.payload)
  }

  useEffect(() => {
    fetchIdp()
  }, [])

  return (
    <div className="my-idp">
      <Breadcrumbs items={breadcrumbs} />
      <div className="header">
        <div className="container">
          {currentIdp.logoUrl ? <img src={currentIdp.logoUrl} alt={currentIdp.logoUrl} /> : null}
          <h1>{currentIdp.names[I18n.locale]}</h1>
        </div>
      </div>
      <div className="container content">
        {Object.keys(roles).length > 0 && <RolesTable roles={roles} />}
        <GeneralInformation idp={currentIdp} isDashboardAdmin={isDashboardAdmin} />
        <Settings idp={currentIdp} isDashboardAdmin={isDashboardAdmin} />
        <ContactPersons idp={currentIdp} isDashboardAdmin={isDashboardAdmin} />
        <ServicesForIdp />
      </div>
    </div>
  )
}

function ContactPersons({ idp, isDashboardAdmin }) {
  if (!idp.contactPersons || idp.contactPersons.length === 0) {
    return null
  }

  const groupedContactPersons = groupBy(idp.contactPersons, (contactPerson) => {
    return `${contactPerson.name}-${contactPerson.emailAddress}-${contactPerson.telephoneNumber}`
  })

  const contactPersons = Object.values(groupedContactPersons).map((roleObjects) => {
    return {
      name: roleObjects[0].name,
      emailAddress: roleObjects[0].emailAddress,
      telephoneNumber: roleObjects[0].telephoneNumber,
      types: [...new Set(roleObjects.map((x) => x.contactPersonType))],
    }
  })

  return (
    <div className="contact-persons">
      <div className="header-with-button">
        <h2>{I18n.t('my_idp.contact')}</h2>

        {isDashboardAdmin && <EditIdpButton />}
      </div>
      <div className="contact-persons-grid">
        {contactPersons.map((contactPerson, i) => (
          <ContactPerson contactPerson={contactPerson} key={i} />
        ))}
      </div>
    </div>
  )
}

function Badge({ enabled }) {
  return (
    <div className={`badge ${enabled ? 'enabled' : 'disabled'}`}>
      {enabled ? I18n.t('boolean.yes') : I18n.t('boolean.no')}
    </div>
  )
}

function Settings({ idp, isDashboardAdmin }) {
  return (
    <div className="settings">
      <div className="header-with-button">
        <h2>{I18n.t('my_idp.settings')}</h2>
        {isDashboardAdmin && <EditIdpButton />}
      </div>
      <table>
        <tbody>
          <tr>
            <td>{I18n.t('my_idp.published_in_edugain')}</td>
            <td>
              <Badge enabled={idp.publishedInEdugain} />
            </td>
          </tr>
          {idp.publishedInEdugain && (
            <tr>
              <td>{I18n.t('my_idp.date_published_in_edugain')}</td>
              <td>{moment(idp.publishInEdugainDate).locale(I18n.locale).format('LLLL')}</td>
            </tr>
          )}
          <tr>
            <td>{I18n.t('my_idp.research_and_scholarship_info')}</td>
            <td>
              <div className="tooltip-container">
                <Badge enabled={idp.connectToRSServicesAutomatically} />
                <Tooltip
                  id="connectToRsServicesAutomatically"
                  text={I18n.t('my_idp.research_and_scholarship_tooltip')}
                />
              </div>
            </td>
          </tr>
          <tr>
            <td>{I18n.t('my_idp.allow_maintainers_to_manage_authz_rules')}</td>
            <td>
              <div className="tooltip-container">
                <Badge enabled={idp.allowMaintainersToManageAuthzRules} />
                <Tooltip
                  id="allowMaintainersToManageAuthzRules"
                  text={I18n.t('my_idp.allow_maintainers_to_manage_authz_rules_tooltip')}
                />
              </div>
            </td>
          </tr>
          <tr>
            <td>{I18n.t('my_idp.displayAdminEmailsInDashboard')}</td>
            <td>
              <div className="tooltip-container">
                <Badge enabled={idp.displayAdminEmailsInDashboard} />
                <Tooltip
                  id="displayAdminEmailsInDashboard"
                  text={I18n.t('my_idp.displayAdminEmailsInDashboardTooltip')}
                />
              </div>
            </td>
          </tr>
          <tr>
            <td>{I18n.t('my_idp.displayStatsInDashboard')}</td>
            <td>
              <div className="tooltip-container">
                <Badge enabled={idp.displayStatsInDashboard} />
                <Tooltip id="displayStatsInDashboard" text={I18n.t('my_idp.displayStatsInDashboardTooltip')} />
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  )
}

function Tooltip({ id, text }) {
  return (
    <div>
      <FontAwesomeIcon icon={faQuestionCircle} data-tip data-for={id} />
      <ReactTooltip id={id} type="info" class="tool-tip" effect="solid" multiline delayHide={250} clickable>
        <span dangerouslySetInnerHTML={{ __html: text }} />
      </ReactTooltip>
    </div>
  )
}

function GeneralInformation({ idp, isDashboardAdmin }) {
  return (
    <div className="general-information">
      <div className="header-with-button">
        <h2>{I18n.t('my_idp.general_information')}</h2>
        {isDashboardAdmin && <EditIdpButton />}
      </div>
      <table>
        <thead>
          <tr>
            <th></th>
            <th>{I18n.t('my_idp.english')}</th>
            <th>{I18n.t('my_idp.dutch')}</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>{I18n.t('my_idp.entity_id')}</td>
            <td className="bold">{idp.id}</td>
            <td className="bold">{idp.id}</td>
          </tr>
          <tr>
            <td>{I18n.t('my_idp.name.general')}</td>
            <td className="bold">{idp.names.en}</td>
            <td className="bold">{idp.names.nl}</td>
          </tr>
          <tr>
            <td>{I18n.t('my_idp.state')}</td>
            <td className="bold">{I18n.t('my_idp.' + idp.state)}</td>
            <td className="bold">{I18n.t('my_idp.' + idp.state)}</td>
          </tr>
          <tr>
            <td>{I18n.t('my_idp.description.general')}</td>
            <td className="bold">{idp.descriptions.en}</td>
            <td className="bold">{idp.descriptions.nl}</td>
          </tr>
          <tr>
            <td>{I18n.t('my_idp.displayName.general')}</td>
            <td className="bold">{idp.displayNames.en}</td>
            <td className="bold">{idp.displayNames.nl}</td>
          </tr>
          <tr>
            <td>{I18n.t('my_idp.organizationURL.general')}</td>
            <td className="bold">
              <div className="tooltip-container">
                <div>{idp.homeUrls.en}</div>
                <Tooltip id="home-url-en" text={I18n.t('my_idp.organizationURL_en_tooltip')} />
              </div>
            </td>
            <td className="bold">
              <div className="tooltip-container">
                <div>{idp.homeUrls.nl}</div>
                <Tooltip id="home-url-nl" text={I18n.t('my_idp.organizationURL_nl_tooltip')} />
              </div>
            </td>
          </tr>
          <tr>
            <td>{I18n.t('my_idp.organizationName.general')}</td>
            <td className="bold">
              <div className="tooltip-container">
                <div>{idp.organisationNames.en}</div>
                <Tooltip id="organisation-name-en" text={I18n.t('my_idp.organizationName_en_tooltip')} />
              </div>
            </td>
            <td className="bold">
              <div className="tooltip-container">
                <div>{idp.organisationNames.nl}</div>
                <Tooltip id="organisation-name-nl" text={I18n.t('my_idp.organizationName_nl_tooltip')} />
              </div>
            </td>
          </tr>
          <tr>
            <td>{I18n.t('my_idp.organizationDisplayName.general')}</td>
            <td className="bold">
              <div className="tooltip-container">
                <div>{idp.organisationDisplayNames.en}</div>
                <Tooltip id="organisation-display-name-en" text={I18n.t('my_idp.organizationDisplayName_en_tooltip')} />
              </div>
            </td>
            <td className="bold">
              <div className="tooltip-container">
                <div>{idp.organisationDisplayNames.nl}</div>
                <Tooltip id="organisation-display-name-nl" text={I18n.t('my_idp.organizationDisplayName_nl_tooltip')} />
              </div>
            </td>
          </tr>
          <tr>
            <td>{I18n.t('my_idp.keywords.general')}</td>
            <td className="bold">{idp.keywords.en}</td>
            <td className="bold">{idp.keywords.nl}</td>
          </tr>
          <tr className="logo-url">
            <td>{I18n.t('my_idp.logo_url')}</td>
            <td>{idp.logoUrl ? <img src={idp.logoUrl} alt={idp.logoUrl} /> : null}</td>
            <td></td>
          </tr>
        </tbody>
      </table>
    </div>
  )
}

function EditIdpButton() {
  return (
    <Link className="c-button" to={'/my-idp/edit'}>
      {I18n.t('my_idp.edit')}
    </Link>
  )
}

function RolesTable({ roles }) {
  return (
    <div className="roles-table">
      <h2>{I18n.t('my_idp.roles')}</h2>
      <p dangerouslySetInnerHTML={{ __html: I18n.t('my_idp.sub_title_html') }}></p>
      <table>
        <thead>
          <tr>
            <th>{I18n.t('my_idp.role')}</th>
            <th>{I18n.t('my_idp.users')}</th>
          </tr>
        </thead>
        <tbody>
          {Object.keys(roles).map((roleName) => (
            <RoleRow roleName={roleName} key={roleName} users={roles[roleName]} />
          ))}
        </tbody>
      </table>
    </div>
  )
}

function RoleRow({ roleName, users }) {
  const names = users
    .map((r) => {
      return r.firstName + ' ' + r.surname
    })
    .sort()
    .join(', ')

  return (
    <tr key={roleName}>
      <td>{roleName}</td>
      <td className="bold">{names}</td>
    </tr>
  )
}