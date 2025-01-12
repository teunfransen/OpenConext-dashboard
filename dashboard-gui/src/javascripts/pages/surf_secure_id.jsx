import React, { useContext, useState } from 'react'
import I18n from 'i18n-js'
import { CurrentUserContext } from '../App'
import { isEmpty } from '../utils/utils'
import SelectWrapper from '../components/select_wrapper'
import { surfSecureIdChangeRequest } from '../api'
import stopEvent from '../utils/stop'
import { setFlash } from '../utils/flash'
import StepUpModal from "../components/step_up_modal"

export default function SurfSecureID({ app }) {
  const { currentUser } = useContext(CurrentUserContext)
  const stepEntity = (currentUser.currentIdp.stepupEntities || []).find((e) => e.name === app.spEntityId)
  const initialLoaLevel = app.minimalLoaLevel || (stepEntity && stepEntity.level) || ''

  const [loaLevel, setLoaLevel] = useState(initialLoaLevel)
  const [showStepUpModal, setShowStepUpModal] = useState(false)
  const isDashboardAdmin = currentUser.dashboardAdmin
  const appHasLoaLevel = !isEmpty(app.minimalLoaLevel)
  const highestLoaLevel = stepEntity && stepEntity.level.endsWith('loa3')
  const loaLevelEquals = stepEntity && stepEntity.level === loaLevel

  let options = []
  if (isEmpty(stepEntity)) {
    options.push({ value: '', display: I18n.t('consent_panel.defaultLoa') })
  }
  options = options.concat(
    currentUser.loaLevels.map((t) => ({
      value: t,
      display: I18n.t(`consent_panel.${t.substring(t.lastIndexOf('/') + 1).toLowerCase()}`),
    }))
  )

  const checkLoaLevel = callback => {
    if (currentUser.currentLoaLevel < 3 && currentUser.dashboardStepupEnabled) {
      setShowStepUpModal(true)
    } else {
      callback();
    }
  }

  function saveRequest(e) {
    stopEvent(e)

    surfSecureIdChangeRequest({ entityId: app.spEntityId, loaLevel: loaLevel, entityType: app.entityType })
      .then((res) => {
        res.json().then((action) => {
          if (action.payload['no-changes']) {
            setFlash(I18n.t('my_idp.no_change_request_created'), 'warning')
          } else {
            setFlash(I18n.t('my_idp.change_request_created', { jiraKey: action.payload.jiraKey }))
          }
          window.scrollTo(0, 0)
        })
      })
      .catch(() => {
        setFlash(I18n.t('my_idp.change_request_failed'), 'error')
        window.scrollTo(0, 0)
      })
  }

  return (
    <div>
      <h2 dangerouslySetInnerHTML={{ __html: I18n.t('ssid_panel.title') }} />
      <p dangerouslySetInnerHTML={{ __html: I18n.t('ssid_panel.subtitle') }} />
      <p className="info" dangerouslySetInnerHTML={{ __html: I18n.t('ssid_panel.subtitle2') }} />
      {isDashboardAdmin && <p className="info" dangerouslySetInnerHTML={{ __html: I18n.t('ssid_panel.subtitle3') }} />}
      <div className="mod-ssid-panel">
        {appHasLoaLevel && <p className="error">{I18n.t('ssid_panel.appHasLoaLevel')}</p>}
        {highestLoaLevel && !appHasLoaLevel && (
          <p className="error" dangerouslySetInnerHTML={{ __html: I18n.t('ssid_panel.highestLoaReached') }} />
        )}
        <section className="change-form">
          <label htmlFor="loa-level">{I18n.t('consent_panel.loa_level')}</label>
          <SelectWrapper
            defaultValue={loaLevel}
            options={options}
            multiple={false}
            inputId="loa-level"
            isDisabled={highestLoaLevel || !isDashboardAdmin || appHasLoaLevel}
            handleChange={(val) => setLoaLevel(val)}
          />
          {isDashboardAdmin && !highestLoaLevel && !appHasLoaLevel && (
            <button
              className={`c-button save ${loaLevelEquals ? 'disabled' : ''}`}
              disabled={loaLevelEquals}
              onClick={e => checkLoaLevel(() => saveRequest(e))}>
              {I18n.t('consent_panel.save')}
            </button>
          )}
        </section>
      </div>
      <StepUpModal
          app={app}
          isOpen={showStepUpModal}
          onClose={() => setShowStepUpModal(false)}
          requiredLoaLevel={3}
      />
    </div>
  )
}
