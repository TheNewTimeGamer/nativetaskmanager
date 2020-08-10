package newtime.util.process;

import java.util.ArrayList;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTR;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.Tlhelp32.PROCESSENTRY32;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.win32.W32APIOptions;

public class ProcessManager {
	
	public static void main(String[] args) {
		new ProcessManager();
	}

	ArrayList<NativeProcess> processes = new ArrayList<NativeProcess>();
	
	public ProcessManager() {
		int count = scanProcesses();
		System.out.println("Scanned: " + count  + " processes");
		for(int i = 0; i < processes.size(); i++) {
			NativeProcess process = processes.get(i);
			if(process.processName.equals("notepad.exe")) {
				process.setPriority(ExtendedKernel32.HIGH_PRIORITY_CLASS);
			}
		}
	}
	
	public int scanProcesses() {
		DWORD pid = new DWORD();
		HANDLE snapshot = Kernel32.INSTANCE.CreateToolhelp32Snapshot(Tlhelp32.TH32CS_SNAPPROCESS, pid);
		PROCESSENTRY32 entry = new PROCESSENTRY32();
		
		int count = 0;
		while(Kernel32.INSTANCE.Process32Next(snapshot, entry)) {
			processes.add(new NativeProcess(entry));
			count++;
		}
		return count;
	}
	
}

interface ExtendedKernel32 extends Kernel32 {
	
	final long ABOVE_NORMAL_PRIORITY_CLASS = 0x00008000;
	final long BELOW_NORMAL_PRIORITY_CLASS = 0x00004000;
	final long HIGH_PRIORITY_CLASS = 0x00000080;
	final long IDLE_PRIORITY_CLASS = 0x00000040;
	final long NORMAL_PRIORITY_CLASS = 0x00000020;
	final long PROCESS_MODE_BACKGROUND_BEGIN = 0x00100000;
	final long PROCESS_MODE_BACKGROUND_END = 0x00200000;
	final long REALTIME_PRIORITY_CLASS = 0x00000100;
	
	ExtendedKernel32 INSTANCE = (ExtendedKernel32)Native.load("kernel32", ExtendedKernel32.class, W32APIOptions.DEFAULT_OPTIONS);
    boolean SetPriorityClass(HANDLE hProcess, DWORD dwPriorityClass);
}

class NativeProcess {

	public final DWORD processID;
	public final String processName;
	
	public NativeProcess(PROCESSENTRY32 entry) {
		this.processID = entry.th32ProcessID;
		this.processName = Native.toString(entry.szExeFile);
	}
	
	public void terminate() throws Win32Exception {
		HANDLE handle = Kernel32.INSTANCE.OpenProcess(Kernel32.PROCESS_TERMINATE, true, this.processID.intValue());	
		if(handle == null) {
			throw new Win32Exception(Native.getLastError());
		}
		if(!Kernel32.INSTANCE.TerminateProcess(handle, 0)) {
			throw new Win32Exception(Native.getLastError());
		}		
	}
	
	public void setPriority(long priority) throws Win32Exception {		
		HANDLE handle = Kernel32.INSTANCE.OpenProcess(Kernel32.PROCESS_SET_INFORMATION, true, this.processID.intValue());
		if(handle == null) {
			throw new Win32Exception(Native.getLastError());
		}
		
		DWORD dPriority = new DWORD(priority);
		
		if(!ExtendedKernel32.INSTANCE.SetPriorityClass(handle, dPriority)) {
			throw new Win32Exception(Native.getLastError());
		}
	}
	
	public void setAffinity(long affinity) throws Win32Exception {
		HANDLE handle = Kernel32.INSTANCE.OpenProcess(Kernel32.PROCESS_SET_INFORMATION, true, this.processID.intValue());
		if(handle == null) {
			throw new Win32Exception(Native.getLastError());
		}
		
		ULONG_PTR uLongPtr = new ULONG_PTR(affinity);
		
		if(!Kernel32.INSTANCE.SetProcessAffinityMask(handle, uLongPtr)) {
			throw new Win32Exception(Native.getLastError());
		}
	}
	
}

