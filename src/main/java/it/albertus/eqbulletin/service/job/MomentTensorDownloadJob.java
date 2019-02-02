package it.albertus.eqbulletin.service.job;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import it.albertus.eqbulletin.model.Earthquake;
import it.albertus.eqbulletin.model.MomentTensor;
import it.albertus.eqbulletin.resources.Messages;
import it.albertus.eqbulletin.service.net.MomentTensorDownloader;

public class MomentTensorDownloadJob extends Job implements DownloadJob<MomentTensor> {

	private final Earthquake earthquake;

	private Optional<MomentTensor> downloadedObject = Optional.empty();

	public MomentTensorDownloadJob(final Earthquake earthquake) {
		super(MomentTensorDownloadJob.class.getSimpleName());
		this.earthquake = earthquake;
		setUser(true);
	}

	@Override
	public IStatus run(final IProgressMonitor monitor) {
		monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
		try {
			downloadedObject = MomentTensorDownloader.download(earthquake);
			monitor.done();
			return Status.OK_STATUS;
		}
		catch (final FileNotFoundException e) {
			return new Status(IStatus.INFO, getClass().getName(), Messages.get("err.job.mt.not.found"), e);
		}
		catch (final IOException e) {
			return new Status(IStatus.WARNING, getClass().getName(), Messages.get("err.job.mt"), e);
		}
		catch (final Exception e) {
			return new Status(IStatus.ERROR, getClass().getName(), Messages.get("err.job.mt"), e);
		}
	}

	@Override
	public Optional<MomentTensor> getDownloadedObject() {
		return downloadedObject;
	}

}
